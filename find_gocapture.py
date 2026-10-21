with open(r"app\src\main\assets\web\index.html", "r", encoding="utf-8") as f:
    text = f.read()

import re
matches = [m.start() for m in re.finditer(r'goCapture', text)]
print("goCapture count:", len(matches))
for pos in matches:
    print(text[max(0, pos-100):min(len(text), pos+150)])
    print("-" * 50)
