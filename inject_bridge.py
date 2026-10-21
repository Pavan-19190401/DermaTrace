import os

html_path = r"app\src\main\assets\web\index.html"
with open(html_path, "r", encoding="utf-8") as f:
    html = f.read()

bridge_script = """
<script>
// DermaTrace Native Android Integration Layer
(function() {
  console.log("[DermaTrace] Initializing Native Web-Android Bridge...");

  window.currentAnalysis = null;
  window.capturedImage = null;

  // Global hooks called from native Kotlin MainActivity
  window.onAndroidBridgeReady = function() {
    console.log("[DermaTrace] AndroidBridge is ready!");
    try {
      if (window.AndroidBridge && window.AndroidBridge.getDashboardData) {
        var data = JSON.parse(window.AndroidBridge.getDashboardData());
        console.log("[DermaTrace] Loaded native dashboard data:", data);
      }
    } catch (e) {
      console.warn("Could not load dashboard data:", e);
    }
  };

  window.onImageCaptured = function(base64) {
    console.log("[DermaTrace] Image captured, sending to ML pipeline...");
    window.capturedImage = base64;
    showAnalysisLoading();
    if (window.AndroidBridge && window.AndroidBridge.analyzeImage) {
      window.AndroidBridge.analyzeImage(base64, null);
    } else {
      // In-browser preview simulation
      setTimeout(function() {
        var mockResult = {
          outcome: { title: "No Visit Needed", level: "green" },
          changeScore: 18.2,
          predictedClass: "nv",
          className: "Melanocytic Nevus (Benign)",
          confidence: 0.92,
          riskTier: "BENIGN",
          asymmetryScore: 0.12,
          borderScore: 0.15,
          colorScore: 0.18,
          diameterMm: 4.2,
          evolutionScore: 18.2,
          plainLanguageRecommendation: "Low change score (18.2%) and confident benign pattern. Lesion appears stable. Continue routine monthly self-monitoring."
        };
        window.onAnalysisComplete(mockResult);
      }, 1500);
    }
  };

  window.onAnalysisComplete = function(result) {
    console.log("[DermaTrace] Analysis complete:", result);
    window.currentAnalysis = result;
    renderAnalysisResult(result);
  };

  window.onAnalysisError = function(err) {
    alert("Analysis Error: " + err);
    hideAnalysisLoading();
  };

  function showAnalysisLoading() {
    var loader = document.getElementById("dt-analysis-overlay");
    if (!loader) {
      loader = document.createElement("div");
      loader.id = "dt-analysis-overlay";
      loader.style.cssText = "position:fixed;inset:0;background:rgba(9,13,22,0.92);backdrop-filter:blur(16px);z-index:99999;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#fff;padding:24px;text-align:center;";
      loader.innerHTML = `
        <div style="width:64px;height:64px;border:3px solid rgba(56,189,248,0.2);border-top-color:#38bdf8;border-radius:50%;animation:dt-spin 1s linear infinite;margin-bottom:20px;"></div>
        <h2 style="font-size:20px;font-weight:600;margin:0 0 8px 0;color:#f8fafc;">Analyzing Skin Lesion...</h2>
        <p style="font-size:13px;color:#94a3b8;margin:0;max-width:280px;line-height:1.5;">Running MobileNetV2 Siamese change detection &amp; 7-class pattern recognition (100% On-Device TFLite)</p>
        <style>@keyframes dt-spin { to { transform: rotate(360deg); } }</style>
      `;
      document.body.appendChild(loader);
    }
    loader.style.display = "flex";
  }

  function hideAnalysisLoading() {
    var loader = document.getElementById("dt-analysis-overlay");
    if (loader) loader.style.display = "none";
  }

  function renderAnalysisResult(res) {
    hideAnalysisLoading();
    var modal = document.getElementById("dt-result-modal");
    if (!modal) {
      modal = document.createElement("div");
      modal.id = "dt-result-modal";
      modal.style.cssText = "position:fixed;inset:0;background:#090d16;z-index:100000;overflow-y:auto;padding:20px 16px;box-sizing:border-box;color:#f8fafc;font-family:system-ui,-apple-system,sans-serif;";
      document.body.appendChild(modal);
    }

    var isGreen = res.outcome.level === "green";
    var isAmber = res.outcome.level === "amber";
    var badgeBg = isGreen ? "rgba(34,197,94,0.15)" : (isAmber ? "rgba(245,158,11,0.15)" : "rgba(239,68,68,0.15)");
    var badgeColor = isGreen ? "#4ade80" : (isAmber ? "#fbbf24" : "#f87171");
    var badgeBorder = isGreen ? "rgba(34,197,94,0.3)" : (isAmber ? "rgba(245,158,11,0.3)" : "rgba(239,68,68,0.3)");

    var imgTag = window.capturedImage ? `<img id="dt-preview-img" src="${window.capturedImage}" style="width:100%;height:220px;object-fit:cover;border-radius:14px;border:1px solid rgba(255,255,255,0.15);margin-bottom:14px;" />` : "";
    var gradCamBtn = res.gradCamBase64 ? `<button id="dt-btn-gradcam" style="height:40px;background:rgba(255,255,255,0.08);border:1px solid rgba(255,255,255,0.2);border-radius:10px;color:#fff;font-size:13px;font-weight:600;cursor:pointer;width:100%;margin-bottom:14px;">Toggle Grad-CAM Heatmap</button>` : "";

    modal.innerHTML = `
      <div style="max-width:440px;margin:0 auto;display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h1 style="font-size:22px;font-weight:700;margin:0;">Analysis Result</h1>
          <button id="dt-close-result" style="background:none;border:none;color:#94a3b8;font-size:22px;cursor:pointer;padding:4px 8px;">✕</button>
        </div>

        ${imgTag}
        ${gradCamBtn}

        <!-- Big Decision Card -->
        <div style="background:${badgeBg};border:1px solid ${badgeBorder};border-radius:16px;padding:18px;">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
            <span style="font-size:12px;text-transform:uppercase;letter-spacing:1px;font-weight:700;color:${badgeColor};">Triaged Outcome</span>
            <span style="font-size:11px;background:${badgeBorder};color:${badgeColor};padding:3px 8px;border-radius:20px;font-weight:600;">${res.riskTier}</span>
          </div>
          <h2 style="font-size:24px;font-weight:800;color:#fff;margin:0 0 8px 0;">${res.outcome.title}</h2>
          <p style="font-size:13px;line-height:1.5;color:#e2e8f0;margin:0;">${res.plainLanguageRecommendation}</p>
        </div>

        <!-- Change Score Gauge -->
        <div style="background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.12);border-radius:16px;padding:16px;">
          <div style="display:flex;justify-content:space-between;align-items:baseline;margin-bottom:8px;">
            <span style="font-size:14px;font-weight:600;color:#e2e8f0;">Siamese Change Score</span>
            <span style="font-size:18px;font-weight:800;color:#38bdf8;">${res.changeScore.toFixed(1)}%</span>
          </div>
          <div style="height:10px;background:rgba(255,255,255,0.1);border-radius:6px;overflow:hidden;position:relative;">
            <div style="width:${Math.min(100, Math.max(0, res.changeScore))}%;height:100%;background:linear-gradient(90deg, #4ade80, #fbbf24 50%, #f87171);border-radius:6px;"></div>
          </div>
          <div style="display:flex;justify-content:space-between;font-size:11px;color:#94a3b8;margin-top:6px;">
            <span>0% (No Change)</span>
            <span>35% Low</span>
            <span>65% High</span>
            <span>100%</span>
          </div>
        </div>

        <!-- Classification & Confidence -->
        <div style="background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.12);border-radius:16px;padding:16px;">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
            <span style="font-size:14px;font-weight:600;color:#e2e8f0;">Predicted Class</span>
            <span style="font-size:13px;color:#38bdf8;font-weight:700;">${(res.confidence * 100).toFixed(1)}% Confidence</span>
          </div>
          <p style="font-size:15px;color:#fff;font-weight:600;margin:0 0 12px 0;">${res.className}</p>

          <!-- ABCDE Breakdown -->
          <h3 style="font-size:13px;color:#94a3b8;margin:0 0 8px 0;text-transform:uppercase;letter-spacing:0.5px;">ABCDE Explainability</h3>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
            <div style="background:rgba(0,0,0,0.25);padding:10px;border-radius:10px;border:1px solid rgba(255,255,255,0.08);">
              <div style="font-size:11px;color:#94a3b8;">Asymmetry (A)</div>
              <div style="font-size:14px;font-weight:700;color:#fff;">${res.asymmetryScore.toFixed(2)}</div>
            </div>
            <div style="background:rgba(0,0,0,0.25);padding:10px;border-radius:10px;border:1px solid rgba(255,255,255,0.08);">
              <div style="font-size:11px;color:#94a3b8;">Border Irreg. (B)</div>
              <div style="font-size:14px;font-weight:700;color:#fff;">${res.borderScore.toFixed(2)}</div>
            </div>
            <div style="background:rgba(0,0,0,0.25);padding:10px;border-radius:10px;border:1px solid rgba(255,255,255,0.08);">
              <div style="font-size:11px;color:#94a3b8;">Color Variance (C)</div>
              <div style="font-size:14px;font-weight:700;color:#fff;">${res.colorScore.toFixed(2)}</div>
            </div>
            <div style="background:rgba(0,0,0,0.25);padding:10px;border-radius:10px;border:1px solid rgba(255,255,255,0.08);">
              <div style="font-size:11px;color:#94a3b8;">Diameter (D)</div>
              <div style="font-size:14px;font-weight:700;color:#fff;">${res.diameterMm.toFixed(1)} mm</div>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <button id="dt-btn-pdf" style="height:48px;border-radius:12px;border:none;background:#38bdf8;color:#090d16;font-size:15px;font-weight:700;cursor:pointer;width:100%;">Export &amp; Share Doctor PDF Report</button>
        <button id="dt-btn-maps" style="height:48px;border-radius:12px;border:1px solid rgba(255,255,255,0.25);background:rgba(255,255,255,0.1);color:#fff;font-size:15px;font-weight:600;cursor:pointer;width:100%;">Find a Dermatologist Near Me</button>

        <!-- Mandatory Disclaimer -->
        <div style="font-size:11px;color:#f87171;line-height:1.45;background:rgba(239,68,68,0.1);border:1px solid rgba(239,68,68,0.25);padding:12px;border-radius:12px;text-align:center;">
          <strong>Clinical Decision Support Disclaimer:</strong> DermaTrace is an automated triage tool, NOT a diagnostic system. It does not replace a doctor.
        </div>
      </div>
    `;
    modal.style.display = "block";

    // Bind modal actions
    document.getElementById("dt-close-result").onclick = function() {
      modal.style.display = "none";
    };

    if (document.getElementById("dt-btn-gradcam")) {
      var showingGrad = false;
      document.getElementById("dt-btn-gradcam").onclick = function() {
        var img = document.getElementById("dt-preview-img");
        if (img && res.gradCamBase64) {
          showingGrad = !showingGrad;
          img.src = showingGrad ? res.gradCamBase64 : window.capturedImage;
          this.textContent = showingGrad ? "Show Original Capture" : "Toggle Grad-CAM Heatmap";
        }
      };
    }

    document.getElementById("dt-btn-pdf").onclick = function() {
      if (window.AndroidBridge && window.AndroidBridge.exportPdfReport) {
        window.AndroidBridge.exportPdfReport(null);
      } else {
        alert("PDF export triggered. Supported in Android native build.");
      }
    };

    document.getElementById("dt-btn-maps").onclick = function() {
      if (window.AndroidBridge && window.AndroidBridge.findDermatologist) {
        window.AndroidBridge.findDermatologist();
      } else {
        window.open("https://www.google.com/maps/search/dermatologist+near+me", "_blank");
      }
    };
  }

  // Intercept button clicks for seamless native Camera / Gallery / PDF triggers
  document.addEventListener("click", function(e) {
    var btn = e.target.closest("button");
    if (!btn) return;
    var txt = btn.textContent.trim().toLowerCase();

    if (txt === "open camera" || txt === "take photo") {
      e.preventDefault();
      e.stopPropagation();
      if (window.AndroidBridge && window.AndroidBridge.capturePhoto) {
        window.AndroidBridge.capturePhoto();
      } else {
        var fi = document.createElement("input");
        fi.type = "file";
        fi.accept = "image/*";
        fi.capture = "environment";
        fi.onchange = function(ev) {
          var f = ev.target.files[0];
          if (f) {
            var reader = new FileReader();
            reader.onload = function(r) { window.onImageCaptured(r.target.result); };
            reader.readAsDataURL(f);
          }
        };
        fi.click();
      }
    } else if (txt === "gallery") {
      e.preventDefault();
      e.stopPropagation();
      if (window.AndroidBridge && window.AndroidBridge.pickGallery) {
        window.AndroidBridge.pickGallery();
      } else {
        var fi2 = document.createElement("input");
        fi2.type = "file";
        fi2.accept = "image/*";
        fi2.onchange = function(ev) {
          var f = ev.target.files[0];
          if (f) {
            var reader = new FileReader();
            reader.onload = function(r) { window.onImageCaptured(r.target.result); };
            reader.readAsDataURL(f);
          }
        };
        fi2.click();
      }
    } else if (txt.includes("export") || txt.includes("pdf") || txt.includes("report")) {
      if (window.AndroidBridge && window.AndroidBridge.exportPdfReport) {
        window.AndroidBridge.exportPdfReport(null);
      }
    }
  }, true);

})();
</script>
"""

if "</body>" in html:
    new_html = html.replace("</body>", bridge_script + "\n</body>")
else:
    new_html = html + bridge_script

with open(html_path, "w", encoding="utf-8") as f:
    f.write(new_html)

as_html_path = r"C:\Users\manik\AndroidStudioProjects\DermaTrace\app\src\main\assets\web\index.html"
with open(as_html_path, "w", encoding="utf-8") as f:
    f.write(new_html)

print("Injected native Android integration script into web assets successfully!")
