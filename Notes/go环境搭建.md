环境安装【Windows】

1、安装go1.8.1.windows-amd64.msi

官网下载，”下一步“方式即可安装，安装完成会自动配置系统的go语言环境变量。输入命令"go version"，如果能够正常显示所安装的版本，说明安装成功



2、安装idea，由于之前已经安装了idea2017.3.2 ultimate版本，所以就在这个基础上继续安装

如果没有安装idea，先从官网下载安装



3、安装idea的go支持插件，可以从idea插件官网下载。

- 这里因为有网络代理上不来官网，从3ms下载的Go-0.13.1947.zip版本。
- 运行idea之后在file ->settings->Plugins菜单下找到Install plugin from disk，找到刚才下载的Go-0.13.1947.zip进行插件安装
  但是可惜提示版本不兼容，怎么办？
- 打开zip包，找到intellij-go-0.13.1947.jar->META-INF->plugin.xml，找到<idea-version since-build="163.7743" until-build="163.*"/>，将版本号换成自己安装的idea对应的版本[idea的版本号在Help->About下查看]。
  本地idea是173.4127，所以替换成<idea-version since-build="173.4127" until-build="173.*"/>
- 更新zip包后重新安装插件，这次成功了



4、新建一个go项目

File->Project->Go就可以创建一个go项目了。

Test：新建一个go文件hello.go

    package main
    import "fmt"
    func main(){
    	fmt.Print("hello,World");
    }

运行结果：

    hello,World

一切OK

但是这时候如果我们自己定义了包的话需要拷贝到安装目录下的src目录下才能被其它代码引用到，很麻烦，如何破？

- 配置gopath
  File->settings->Language & Frameworks->Go->Go Libraies，在Project Libraries下面添加当前项目路径

5、自定义包

在项目的源码路径（这里我们创建了一个src目录作为源码路径）下创建包example，然后创建一个go文件test.go

    package example
    
    import (
    	"fmt"
    )
    
    func MyTest()  {
    	fmt.Println("这是我自己的方法")
    }

目录结构如下

项目路径

	|—src

		|—example

			|—test.go

		hello.go

hello.go就是程序的主入口：

    package main
    
    import (
    	"example"
    )
    
    func main() {
    	example.MyTest()
    }

执行代码，可以看到程序正常运行了
