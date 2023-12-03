sudo apt-get install -y qemu-kvm binfmt-support qemu-user-static # Install the qemu packages
sudo apt-get install -y gcc-arm-linux-gnueabihf libc6-dev-armhf-cross qemu-user-static qemu-system-i386
docker run --rm --privileged multiarch/qemu-user-static --reset -p yes # This step will execute the registering scripts

docker run --rm -t arm64v8/ubuntu uname -m # Testing the emulation environment
#aarch64