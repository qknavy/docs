##                                 OpenSSL操作证书

* [OpenSSL官网](https://www.openssl.org/source/)

### 1、openssl 简介

openssl 是目前最流行的 SSL 密码库工具，其提供了一个通用、健壮、功能完备的工具套件，用以支持SSL/TLS 协议的实现。



#### 构成部分

* 密码算法库

* 密钥和证书封装管理功能

* SSL通信API接口


#### 用途

* 建立 RSA、DH、DSA key 参数
* 建立 X.509 证书、证书签名请求(CSR)和CRLs(证书回收列表)
* 计算消息摘要
* 使用各种 Cipher加密/解密
* SSL/TLS 客户端以及服务器的测试
* 处理S/MIME 或者加密邮件



### 2、RSA密钥操作

> 默认情况下，openssl 输出格式为 PKCS#1-PEM

#### 生成密钥

##### 生成RSA私钥(无加密)

```
openssl genrsa -out rsa_private.key 2048
```

##### 生成RSA公钥

```
openssl rsa -in rsa_private.key -pubout -out rsa_public.key
```

##### 生成RSA私钥(使用aes256加密)

```
openssl genrsa -aes256 -passout pass:111111 -out rsa_aes_private.key 2048
```

> 其中 passout 代替shell 进行密码输入，否则会提示输入密码；生成加密后的内容如：

```
-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: AES-256-CBC,5584D000DDDD53DD5B12AE935F05A007
Base64 Encoded Data
-----END RSA PRIVATE KEY-----
```

##### 此时若生成公钥，需要提供密码

```
openssl rsa -in rsa_aes_private.key -passin pass:111111 -pubout -out rsa_public.key
```

> 其中 passout 代替shell 进行密码输入，否则会提示输入密码；



#### 转换命令

##### 私钥转非加密

```
openssl rsa -in rsa_aes_private.key -passin pass:111111 -out rsa_private.key
```

##### 私钥转加密

```
openssl rsa -in rsa_private.key -aes256 -passout pass:111111 -out rsa_aes_private.key
```

##### 私钥PEM转DER

```
openssl rsa -in rsa_private.key -outform der-out rsa_aes_private.der
```

> -inform和-outform 参数制定输入输出格式，由der转pem格式同理



#### 查看私钥明细

```
openssl rsa -in rsa_private.key -noout -text
```

> 使用-pubin参数可查看公钥明细





##### 2、openssl list-message-digest-commands(消息摘要命令)

###### dgst: dgst用于计算消息摘要

```
openssl dgst [args]
```

* -hex

  以16进制形式输出摘要

* -binary        

  以二进制形式输出摘要

* -sign file

* 以私钥文件对生成的摘要进行签名
  ​        1.4) -verify file    
  ​        使用公钥文件对私钥签名过的摘要文件进行验证 
  ​        1.5) -prverify file  
  ​        以私钥文件对公钥签名过的摘要文件进行验证
  ​        verify a signature using private key in file
  ​        1.6) 加密处理
  ​            1.6.1) -md5: MD5 
  ​            1.6.2) -md4: MD4         
  ​            1.6.3) -sha1: SHA1 
  ​            1.6.4) -ripemd160
  ​    example1: 用SHA1算法计算文件file.txt的哈西值，输出到stdout
  ​    openssl dgst -sha1 file.txt
  ​    example2: 用dss1算法验证file.txt的数字签名dsasign.bin，验证的private key为DSA算法产生的文件dsakey.pem
  ​    openssl dgst -dss1 -prverify dsakey.pem -signature dsasign.bin file.txt



##### 3、openssl list-cipher-commands (Cipher命令的列表)

* aes-128-cbc
* aes-128-ecb
* aes-192-cbc
* aes-192-ecb
* aes-256-cbc
* aes-256-ecb
* base64
* bf
* bf-cbc
* bf-cfb
* bf-ecb
* bf-ofb
* cast
* cast-cbc
* cast5-cbc
* cast5-cfb
* cast5-ecb
* cast5-ofb
* des
* des-cbc
* des-cfb
* des-ecb
* des-ede
* des-ede-cbc
* des-ede-cfb
* des-ede-ofb
* des-ede3
* des-ede3-cbc
* des-ede3-cfb
* des-ede3-ofb
* des-ofb
* des3
* desx
* rc2
* rc2-40-cbc
* rc2-64-cbc
* rc2-cbc
* rc2-cfb
* rc2-ecb
* rc2-ofb

    * rc4

    * rc4-40