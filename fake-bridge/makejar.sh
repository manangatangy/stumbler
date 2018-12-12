rm -rf build
mkdir build
cd build
cp -rp ../bin/au .
cp -rp ../web-src/* .
chmod -R 777 au
jar -xf ../libs/commons-lang-2.3.jar
jar -xf ../libs/gson-2.2.2.jar
rm META-INF/MANIFEST.MF
rm META-INF/LICENSE.txt
rm META-INF/NOTICE.txt
jar cfe HttpAdbBridge.jar au.com.sensis.whereis.fake.bridge.HttpAdbBridge .

