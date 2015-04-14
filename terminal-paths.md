cd /home/joel/repos/IdeaProjects/appinventor-sources && ./startDev.sh
cd /home/joel/repos/IdeaProjects/appinventor-sources/appinventor/ && ant comps
cd /home/joel/repos/IdeaProjects/appinventor-sources && ./startUp2.sh
cd /home/joel/repos/IdeaProjects/appinventor-sources && ./startDev-packageAPK.sh
cd /home/joel/repos/IdeaProjects/appinventor-sources

# Make video screen captures

adb shell screenrecord --bit-rate 8000000 --time-limit 30 /sdcard/kitkat.mp4

me@jhtong.org