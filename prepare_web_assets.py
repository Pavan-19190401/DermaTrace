import os
import shutil

web_dir = r"c:\Users\manik\OneDrive\Desktop\DermaAppD\app\src\main\assets\web"
os.makedirs(web_dir, exist_ok=True)

# Also ensure C:\Users\manik\AndroidStudioProjects\DermaTrace\app\src\main\assets\web exists
as_web_dir = r"C:\Users\manik\AndroidStudioProjects\DermaTrace\app\src\main\assets\web"
os.makedirs(as_web_dir, exist_ok=True)

# Read standalone_preview.html from scratch
with open("standalone_preview.html", "r", encoding="utf-8") as f:
    content = f.read()

# Save as base index.html
with open(os.path.join(web_dir, "index.html"), "w", encoding="utf-8") as f:
    f.write(content)
with open(os.path.join(as_web_dir, "index.html"), "w", encoding="utf-8") as f:
    f.write(content)

print("Saved base index.html to both asset folders. Length:", len(content))
