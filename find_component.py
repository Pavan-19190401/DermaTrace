with open(r"app\src\main\assets\web\index.html", "r", encoding="utf-8") as f:
    text = f.read()

import re
# Find script with Component
comp_match = re.search(r'class Component extends DCLogic.*?constructor\(p\)\{', text, re.DOTALL)
if comp_match:
    print("Found Component class!")
    start = comp_match.start()
    print("Snippet around Component definition:")
    print(text[start:start+1000])
else:
    print("Component not found")
