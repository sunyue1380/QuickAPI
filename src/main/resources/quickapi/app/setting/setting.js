app.register.controller("settingController", function ($scope, $rootScope, $storageService) {
    $scope.nginxConf = "http {\n" +
        "    include       mime.types;\n" +
        "    default_type  application/octet-stream;\n" +
        "    sendfile        on;\n" +
        "    keepalive_timeout  65;\n" +
        "\n" +
        "    server {\n" +
        "        listen       80;\n" +
        "        server_name  localhost;\n" +
        "\t\t\n" +
        "        client_max_body_size 20m;\n" +
        "        location ^~/quickapi/ {\n" +
        "            add_header Access-Control-Allow-Origin *;\n" +
        "            add_header Access-Control-Allow-Headers \"Origin, X-Requested-With, Content-Type, Accept\";\n" +
        "            add_header Access-Control-Allow-Methods \"GET, POST, OPTIONS\";\n" +
        "\n" +
        "            proxy_set_header Host $host;\n" +
        "            proxy_set_header X-Real-IP $remote_addr;\n" +
        "            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n" +
        "            proxy_set_header X-NginX-Proxy true;\n" +
        "\n" +
        "            proxy_connect_timeout 300;\n" +
        "            proxy_send_timeout 300;\n" +
        "            proxy_read_timeout 300;\n" +
        "            send_timeout  300;\n" +
        "\n" +
        "            proxy_pass http://127.0.0.1:8080/quickapi/;\n" +
        "        }\n" +
        "    }\n" +
        "}";

    $scope.settings = $storageService.settings;
    $scope.$watch("settings",function(newValue,oldValue){
        $storageService.settings = newValue;
    },true);

    //全局头部
    $scope.globalHeader = {
        "key":"",
        "value":"",
        "remark":"",
    };
    $scope.addGlobalFields = [
        {
            "label":"key",
            "type":"input",
            "inputType":"text",
            "model":"key",
            "required":true,
            "placeholder":"头部key"
        },
        {
            "label":"value",
            "type":"input",
            "inputType":"text",
            "model":"value",
            "required":true,
            "placeholder":"头部value"
        },
        {
            "label":"备注",
            "type":"input",
            "inputType":"text",
            "model":"remark",
            "placeholder":"备注"
        },
        {
            "label":"",
            "type":"buttons",
            "buttons":[
                {
                    "name":"添加全局头部",
                    "class":"'is-primary'",
                    "click":function(data){
                        if(typeof(data.key)=="undefined"||typeof(data.value)=="undefined"){
                            return;
                        }
                        if(data.key===""||data.value===""){
                            return;
                        }
                        $scope.settings.globalHeaders.push(angular.copy(data));
                        $scope.globalHeader = {
                            "key":"",
                            "value":"",
                            "remark":"",
                        };
                    }
                }
            ]
        }
    ];

    /**全局头部字段列表*/
    $scope.globalHeaderFields = [
        {
            "key": "key",
            "value": "头部key"
        },
        {
            "key": "value",
            "value": "头部value"
        },
        {
            "key": "remark",
            "value": "备注"
        },
        {
            "key": "operation",
            "value": "操作",
            "type": "buttons",
            "buttons": [
                {
                    "name":"删除",
                    "class":"is-primary is-danger",
                    "click":function(globalHeader,index){
                        if(confirm("确认删除该全局头部吗?")){
                            $scope.settings.globalHeaders.splice(index,1);
                        }
                    }
                }
            ]
        }
    ];

    //环境设置
    $scope.environment = {};
    $scope.addEnvironmentFields = [
        {
            "label":"名称",
            "type":"input",
            "inputType":"text",
            "model":"name",
            "required":true,
            "placeholder":"环境名称,名称不可重复"
        },
        {
            "label":"地址",
            "type":"input",
            "inputType":"text",
            "model":"address",
            "required":true,
            "placeholder":"请带上http(s)前缀,例如http://127.0.0.1:9000"
        },
        {
            "label":"",
            "type":"buttons",
            "buttons":[
                {
                    "name":"添加环境",
                    "class":"'is-primary'",
                    "click":function(environment){
                        if(typeof(environment.name)=="undefined"||typeof(environment.address)=="undefined"){
                            return;
                        }
                        if(null==environment.name||environment.name===""||null==environment.address||environment.address===""){
                            return;
                        }
                        let exist = $scope.settings.environments.some(e=> {
                            if (environment.name === e.name) {
                                return true;
                            }
                        });
                        if(!exist){
                            $scope.settings.environments.push(environment);
                        }
                    }
                }
            ]
        }
    ];

    $scope.environmentFields = [
        {
            "key": "name",
            "value": "名称"
        },
        {
            "key": "address",
            "value": "地址"
        },
        {
            "key": "operation",
            "value": "操作",
            "type": "buttons",
            "buttons": [
                {
                    "name":"启用",
                    "class":"is-primary",
                    "show":function(data){
                        return !$scope.settings.currentEnvironment||($scope.settings.currentEnvironment&&data.name!==$scope.settings.currentEnvironment.name);
                    },
                    "click":function(data){
                        $scope.settings.currentEnvironment = data;
                    }
                },
                {
                    "name":"禁用",
                    "class":"is-danger",
                    "show":function(data){
                        return $scope.settings.currentEnvironment&&data.name===$scope.settings.currentEnvironment.name;
                    },
                    "click":function(){
                        $scope.settings.currentEnvironment = null;
                    }
                },
                {
                    "name":"删除",
                    "class":"is-danger",
                    "click":function(data,index){
                        if(confirm("确认删除吗?")){
                            $scope.settings.environments.splice(index,1);
                        }
                    }
                }
            ]
        }
    ];

    //按钮组
    $scope.buttons = [
        {
            "name":"清空最近使用记录",
            "click":function(){
                if(confirm("确认清空最近使用记录吗?")){
                    $storageService.recentUsed = [];
                    alert("清空完成!");
                }
            }
        },
        {
            "name":"清空收藏记录列表",
            "click":function(){
                if(confirm("确认清空收藏记录列表吗?")){
                    $storageService.collectionList = [];
                    alert("清空完成!");
                }
            }
        },
        {
            "name":"清空所有数据",
            "click":function(){
                if(confirm("确认清空所有数据吗?")){
                    localStorage.removeItem(location.host+location.pathname);
                    location.reload();
                }
            }
        },
        {
            "name":"导出数据",
            "click":function(){
                let keys = Object.getOwnPropertyNames($storageService);
                let exportData = {};
                for(let i=0;i<keys.length;i++){
                    exportData[keys[i]] = $storageService[keys[i]];
                }
                $rootScope.copyToClipBoard(JSON.stringify(exportData));
                alert("数据已经复制到剪贴板上!");
            }
        },
        {
            "name":"导入数据",
            "click":function(){
                let importData = prompt("请输入导入数据");
                if(importData==null||importData==""){
                    return;
                }
                let importDataObject = JSON.parse(importData);
                for(let prop in importDataObject){
                    $storageService[prop] = importDataObject[prop];
                }
                alert("数据导入成功!");
            }
        }
    ];

    $scope.settingButtons = [
        {
            "show":function(){
                return !$scope.settings.showAPIEntity;
            },
            "class":"is-primary",
            "name":"显示接口实体类信息",
            "click":function(){
                $scope.settings.showAPIEntity = true;
            }
        },
        {
            "show":function(){
                return $scope.settings.showAPIEntity;
            },
            "class":"is-danger",
            "name":"隐藏接口实体类信息",
            "click":function(){
                $scope.settings.showAPIEntity = false;
            }
        },
        {
            "show":function(){
                return !$scope.settings.showEntity;
            },
            "class":"is-primary",
            "name":"显示实体类信息",
            "click":function(){
                $scope.settings.showEntity = true;
            }
        },
        {
            "show":function(){
                return $scope.settings.showEntity;
            },
            "class":"is-danger",
            "name":"隐藏实体类信息",
            "click":function(){
                $scope.settings.showEntity = false;
            }
        },
        {
            "show":function(){
                return !$scope.settings.showRecentUsed;
            },
            "class":"is-primary",
            "name":"显示最近使用历史记录",
            "click":function(){
                $scope.settings.showRecentUsed = true;
            }
        },
        {
            "show":function(){
                return $scope.settings.showRecentUsed;
            },
            "class":"is-danger",
            "name":"隐藏最近使用历史记录",
            "click":function(){
                $scope.settings.showRecentUsed = false;
            }
        },
    ];
});