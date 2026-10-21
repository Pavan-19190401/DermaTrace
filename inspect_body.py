with open("dc_template.html", "r", encoding="utf-8") as f:
    tpl = f.read()

import re
# Remove <helmet>...</helmet>
body_content = re.sub(r'<helmet>.*?</helmet>', '', tpl, flags=re.DOTALL)
print("Body content length:", len(body_content))

# Look for all major sections in body_content
sections = re.findall(r'<div[^>]*class="[^"]*"[^>]*>', body_content)
print(f"Total main divs: {len(sections)}")
with open("extracted_body.html", "w", encoding="utf-8") as f_out:
    f_out.write(body_content)
