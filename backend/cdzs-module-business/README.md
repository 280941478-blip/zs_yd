# 项目业务模块

项目业务代码统一在本模块按领域分包。入口已在父 POM 注册，并由 cdzs-server 引入。

调用链：Controller → Service 接口 → ServiceImpl → Mapper。

完整目录、职责、前端对应位置及权限配置见 [业务模块开发约定](../../docs/业务模块开发约定.md)。
