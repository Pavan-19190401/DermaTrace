with open(r"app\src\main\assets\web\index.html", "r", encoding="utf-8") as f:
    text = f.read()

pos = text.find('value="{{ v.capture }}"')
if pos != -1:
    print(text[pos:pos+1500])
