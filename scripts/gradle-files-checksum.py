import os
import hashlib
import sys

def main():
    # make a hash object
    hashObj = hashlib.sha1()

    for root, dirs, files in os.walk("."):
        for name in files:
            if name == "build.gradle" or name == "dependencies.gradle":
                filePath = os.path.join(root, name)
                print(filePath)
                with open(filePath,'rb') as file:
                    # loop till the end of the file
                    chunk = 0
                    while chunk != b'':
                        # read only 1024 bytes at a time
                        chunk = file.read(1024)
                        hashObj.update(chunk)

    checksum = hashObj.hexdigest()
    print("Check sum = " + checksum)

    checksumFilePath = str(sys.argv[1])
    print("File path: " + checksumFilePath)
    with open(checksumFilePath, 'x') as f:
        f.write(checksum)

if __name__ == "__main__":
    main()
