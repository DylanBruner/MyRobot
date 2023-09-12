import os, shutil

# loop through the folders in dylanbruner and add any non-empty folders to the list
folders = []
# walk through the dylanbruner folder
for root, dirs, files in os.walk("dylanbruner"):
    # if there are files in the folder
    if len(files) > 0:
        # add the folder to the list
        folders.append(root.replace("\\", "/").replace("dylanbruner/", ""))
folders.remove("dylanbruner")
command = "javac -cp lib/*; -Xlint:unchecked -d bin -sourcepath dylanbruner dylanbruner/MyRobot.java dylanbruner/*.java [INSERT]".replace(
    "[INSERT]", " ".join(["dylanbruner/" + folder + "/*.java" for folder in folders])
)
r = os.system(command)
shutil.copytree("bin/dylanbruner", "C:/robocode/robots/dylanbruner", dirs_exist_ok=True)
exit(r)
