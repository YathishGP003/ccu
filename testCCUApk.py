import zipfile
import os
import json

zip_file_path = "./CCU_prod_2.18.3.apk"
unzip_file_path = "extract"
with zipfile.ZipFile(zip_file_path, 'r') as zip_ref:
    zip_ref.extractall(unzip_file_path)


def check_if_valid_json(myjson):
    try:
        with open(myjson) as json_read:
            json_object = json.load(json_read)
    except ValueError as e:
        return False
    return True


# Loop in to directory to fetch all the files
models_path = os.path.join(unzip_file_path, "assets", "assets", "75f", "models")
for root, dirs, files in os.walk(models_path):
    for file in files:
        if file.endswith('.json'):
            json_file_path = os.path.join(root, file)
            is_valid_json = check_if_valid_json(json_file_path)
            if not is_valid_json:
                print (json_file_path)


# -v 2.15.9  -o folderpath
