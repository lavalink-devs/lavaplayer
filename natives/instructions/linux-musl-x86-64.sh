docker run --rm -it alpine sh

apk update && apk upgrade
apk add git cmake build-base autoconf openjdk8 wget

wget https://mirror2.sandyriver.net/pub/software/gnu/automake/automake-1.15.1.tar.gz
tar -zxvf automake-1.15.1.tar.gz
cd automake-1.15.1
./configure
make install

git clone https://github.com/Walkyst/lavaplayer-natives-fork
cd lavaplayer-natives-fork
chmod +x ./gradlew
cd natives
../gradlew load
touch opus/opus-1.3/aclocal.m4 configure
../gradlew compileNatives