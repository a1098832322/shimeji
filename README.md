# Shimeji  
![](https://img.shields.io/badge/Version-v1.0.3beta2-green.svg)  

基于Shimeji ee项目构建的一个个人桌宠小项目  

[Shimeji ee官方网站](http://kilkakon.com/shimeji/)

### 运行及打包方法

#### Run:
1. 运行`Main.java`内的`main`方法以启动项目。

#### Maven打包
1. 执行`mvn clean install`进行项目打包。

**注意：需要先指定`Constant`中的`isDevEnvironment`值，`true`为开发环境，方便在IDE下调试，当编译打包时需要先改为`false`。**

#### 仅运行查看项目demo
1. 下载项目zip压缩包。
2. 配置JRE运行环境（已有环境请忽略）。
3. 打开`demo`文件夹，双击运行`shimeji.jar`或双击运行“点我运行.cmd”即可成功运行项目demo。
4. ※ demo版本为1.0.2-Release版，也是支持在线更新的，除非后续对更新功能进行了大幅度修改（会在此备注），需要重新下载完整包，否则均可通过在线更新完成更新。

---  

## 版本更新

[请查看版本更新日志](Updatelog.md)