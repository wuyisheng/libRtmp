# libRtmp

Hi,
 This is an android library of rtmp,include following:

- record camera

- push rtmp stream

- receive rtmp stream and play in local

## How to start

### create self-test rtmp service

#### linux

```shell
docker pull jasonrivers/nginx-rtmp
docker run -it -p 1935:1935 -p 8080:8080 jasonrivers/nginx-rtmp /bin/sh
#./opt/nginx/sbin/nginx
```

#### mac

```shell
brew install docker docker-machine
docker-machine create -d virtualbox default
eval “$(docker-machine env dev)”
docker run hello-world
docker-machine ssh default
```

in ssh

```shell
docker pull jasonrivers/nginx-rtmp
docker run -it -p 1935:1935 -p 8080:8080 jasonrivers/nginx-rtmp /bin/sh
#./opt/nginx/sbin/nginx
```

more detail refer to [MY BLOG](http://blog.csdn.net/yeshennet/article/details/72240465)


### start push steam

firstly , star this project :)


TOWRITE