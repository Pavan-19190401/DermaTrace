with open(r"app\src\main\assets\web\index.html", "r", encoding="utf-8") as f:
    text = f.read()

idx = text.find("class Component extends DCLogic {")
print("Position:", idx)
if idx != -1:
    print("Found exact Component class! Snippet:")
    print(text[idx:idx+1500])
