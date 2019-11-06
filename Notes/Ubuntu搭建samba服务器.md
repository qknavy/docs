# Ubuntu搭建samba服务器
> 确认当前机器没有安装samba

[链接](https://www.linuxidc.com/Linux/2018-11/155466.htm)


## 安装步骤
* 更新当前软件
sudo apt-get upgrade  
sudo apt-get update  
sudo apt-get dist-upgrade

* 安装samba
> apt-get install samba samba-common

* 创建一个用于分享的samba目录
> mkdir /datas/shares

* 给创建的这个目录设置权限
> chmod 777 /datas/shares

* 添加用户(下面的root是我的用户名，之后会需要设置samba的密码)。
> sudo smbpasswd -a root

* 配置samba的配置文件
> sudo nano /etc/samba/smb.conf

在配置文件smb.conf的最后添加下面的内容：
```
[share]
comment = share folder
browseable = yes
path = /datas/shares
create mask = 0700
directory mask = 0700
valid users = linuxidc
force user = linuxidc
force group = linuxidc
public = yes 
available = yes 
writable = yes
```

* 重启samba服务器
> sudo service smbd restart
> 如果是阿里云上搭建，需要设置端口规则445、137、138、139



[samba配置](https://blog.csdn.net/xg38241415109/article/details/78933949)


