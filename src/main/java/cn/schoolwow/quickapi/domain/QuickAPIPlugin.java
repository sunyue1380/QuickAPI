package cn.schoolwow.quickapi.domain;

import cn.schoolwow.quickapi.plugin.AngularJsPlugin;
import cn.schoolwow.quickapi.plugin.MarkdownPlugin;
import cn.schoolwow.quickapi.plugin.Plugin;
import cn.schoolwow.quickapi.plugin.SwaggerPlugin;

/**QuickAPI插件部分*/
public enum QuickAPIPlugin {
    SWAGGER(new SwaggerPlugin()),
    ANGULAR_JS(new AngularJsPlugin()),
    MARKDOWN(new MarkdownPlugin());

    public Plugin plugin;

    QuickAPIPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
}
