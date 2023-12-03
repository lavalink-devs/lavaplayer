docker run --rm -it ubuntu:18.04 bash

apt update && apt upgrade
apt install -y git cmake build-essential autoconf openjdk-17-jdk wget nano

wget http://ftp.de.debian.org/debian/pool/main/a/automake-1.16/automake_1.16.1-4_all.deb
dpkg -i automake_1.16.1-4_all.deb

git clone https://github.com/markozajc/lavaplayer
cd lavaplayer
chmod +x ./gradlew
cd natives

nano build.gradle >> ext.mpg123Version = '1.32.3'

../gradlew load

# comment out PKG_CHECK_MODULES: L

# downgrade cmake version to 3.10

apt install libsamplerate-dev

touch opus/opus-1.4/aclocal.m4 configure
../gradlew compileNatives