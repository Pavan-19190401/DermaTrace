import zipfile
import os
import shutil

zip_path = r"c:\Users\manik\Downloads\DermaTrace-Android-Studio.zip"
dest_as = r"C:\Users\manik\AndroidStudioProjects\DermaTrace"
dest_local = r"c:\Users\manik\OneDrive\Desktop\DermaAppD"

with zipfile.ZipFile(zip_path, 'r') as z:
    for item in z.infolist():
        # Skip .gradle cache files inside zip to keep clean
        if item.filename.startswith('.gradle/8') or item.filename.startswith('.gradle/kotlin'):
            continue
        # Extract to both destinations
        z.extract(item, dest_as)
        z.extract(item, dest_local)

print("Extracted DermaTrace-Android-Studio.zip to both AndroidStudioProjects and DermaAppD successfully!")
