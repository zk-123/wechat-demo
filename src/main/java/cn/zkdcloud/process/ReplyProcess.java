package cn.zkdcloud.process;

import cn.zkdcloud.annotation.MessageProcess;
import cn.zkdcloud.component.menu.Menu;
import cn.zkdcloud.component.menu.MenuType;
import cn.zkdcloud.component.menu.button.NormalButton;
import cn.zkdcloud.component.menu.button.ViewButton;
import cn.zkdcloud.component.message.AbstractResponseMessage;
import cn.zkdcloud.component.message.acceptMessage.Event;
import cn.zkdcloud.component.message.acceptMessage.eventMessage.SubscribeEventMessage;
import cn.zkdcloud.component.message.acceptMessage.normalMessage.AcceptImageMessage;
import cn.zkdcloud.component.message.acceptMessage.normalMessage.AcceptTextMessage;
import cn.zkdcloud.component.message.acceptMessage.normalMessage.AcceptVoiceMessage;
import cn.zkdcloud.component.message.responseMessage.ResponseImageMessage;
import cn.zkdcloud.component.message.responseMessage.ResponseNewsMessage;
import cn.zkdcloud.component.message.responseMessage.ResponseTextMessage;
import cn.zkdcloud.component.user.UserInfo;
import cn.zkdcloud.core.*;
import cn.zkdcloud.core.TemplateComponent.TemplateMessage;
import cn.zkdcloud.util.WeChatUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 消息回复+微信介入(只列出部分，其它可以参照https://github.com/zk-123/weChat,设计出自己的微信公众号)
 *
 * @author zk
 * @version 2017/9/5
 */
@MessageProcess
@RestController
public class ReplyProcess {

    private static Logger logger = Logger.getLogger(ReplyProcess.class);

    /**
     * 用户管理组件
     */
    private UserManagerComponent userManagerComponent = UserManagerComponent.getInstance();

    /**
     * 菜单管理组件
     */
    private MenuComponent menuComponent = MenuComponent.getInstance();

    /**
     * 模板组件
     */
    private TemplateComponent templateComponent = TemplateComponent.getInstance();

    /**
     * 网页认证组件
     */
    private Oauth2AuthorizeComponent authorizeComponent = Oauth2AuthorizeComponent.getInstance();

    /**
     * 微信校验介入
     *
     * @param signature signature
     * @param timestamp timestamp
     * @param nonce     nonce
     * @param echostr   echostr
     * @return ret
     */
    @GetMapping(value = "/wechatOn")
    public String involved(@RequestParam("signature") String signature, @RequestParam("timestamp") String timestamp,
                           @RequestParam("nonce") String nonce, @RequestParam("echostr") String echostr) {
        return WeChatUtil.confirm(signature, timestamp, nonce, echostr);
    }

    /**
     * 被动消息处理
     *
     * @param request request
     * @return messageRet
     */
    @PostMapping(value = "/wechatOn")
    public String dispatch(HttpServletRequest request) {
        return MessageComponent.getInstance().doAdapter(request);
    }

    /**
     * 关注/取消关注处理
     *
     * @param message message
     * @return responseRet
     */
    public AbstractResponseMessage subscribe(SubscribeEventMessage message) {
        if (Event.SUBSCRIBE == message.getEvent()) {
            return new ResponseTextMessage(showMenu());
        } else {
            return new ResponseTextMessage("取消订阅了");
        }
    }

    /**
     * 一般的文本消息test
     *
     * @param message textMessage
     * @return responseRet
     */
    public AbstractResponseMessage replyText(AcceptTextMessage message) {
        String receiveText = message.getContent();
        AbstractResponseMessage responseMessage;

        if (receiveText.startsWith("remark-")) {
            String remarkName = receiveText.substring(receiveText.indexOf("remark-"));
            if (userManagerComponent.remarkUser(message.getFromUserName(), remarkName)) {
                responseMessage = new ResponseTextMessage("设置成功");
            } else {
                responseMessage = new ResponseTextMessage("设置失败");
            }
        } else if (receiveText.equals("make-menu")) {
            markMenu();
            responseMessage = new ResponseTextMessage("成功生成菜单，重新关注后可看到效果");
        } else if (receiveText.equals("delete-menu")) {
            menuComponent.deleteMenu();
            responseMessage = new ResponseTextMessage("成功删除菜单,重新关注后可看到效果");
        } else if (receiveText.equals("send-template")) {
            TemplateMessage templateMessage = templateComponent.getTemplateByName("话费提醒模板");//使用前需去公众号设置
            if (null != templateMessage) {
                templateMessage.setTouserAndUrl(message.getFromUserName(), "http://www.baidu.com");
                templateMessage.addData("first", "话题提醒", "#000000");//颜色必须6位，不能简写
                templateMessage.addData("money", "120.5");//默认黑色
                templateMessage.addData("paly", "25.6");
                templateMessage.addData("donate", "69.2");
                templateMessage.addData("remark", "您的话费很充裕，不用续交话费了");
                templateComponent.sendTemplateMessage(templateMessage);
                return null;//不返回信息
            }
            responseMessage = new ResponseTextMessage("模板消息有错误了");
        } else if (receiveText.equals("get-snsapi-base")) {
            responseMessage = new ResponseTextMessage("由于事先没有设置url跳转地址，所以不能用，" +
                    "如需设置，请参考 https://github.com/zk-123/weChat 下的说明。该url：" + Oauth2AuthorizeComponent.SNSAPI_BASE_URL);
        } else if (receiveText.equals("get-snsapi_userinfo")) {
            responseMessage = new ResponseTextMessage("由于事先没有设置url跳转地址，所以不能用，" +
                    "如需设置，请参考 https://github.com/zk-123/weChat 下的说明。该url：" + Oauth2AuthorizeComponent.SNSAPI_USERINFO_URL);
        } else if (receiveText.equals("get-myinfo")) {
            UserInfo userInfo = userManagerComponent.getUserInfoByOpenId(message.getFromUserName());
            if (null != userInfo) {
                responseMessage = new ResponseTextMessage("nickName :" + userInfo.getNickname() + "remark," + userInfo.getRemark()
                        + ",sex" + userInfo.getSex() + "subscribeTime," + userInfo.getSubscribe() + "等等...");
            } else {
                responseMessage = null;
            }
        } else {
            responseMessage = new ResponseTextMessage("你的消息是：" + message.getContent());
        }
        return responseMessage;
    }

    /**
     * 接受图片消息，并顺便返回该图片
     *
     * @param message imageMessage
     * @return responseRet
     */
    public AbstractResponseMessage photoMessage(AcceptImageMessage message) {
        ResponseImageMessage imageMessage = new ResponseImageMessage();
        imageMessage.setImage(ResponseImageMessage.Image.getImage(message.getMediaId()));
        return imageMessage;
    }

    /**
     * 接受语音消息，返回图文消息
     *
     * @return responseRet
     */
    public AbstractResponseMessage voiceMessage(AcceptVoiceMessage message) {
        ResponseNewsMessage newsMessage = new ResponseNewsMessage();
        newsMessage.addArticle("语音识别结果",message.getRecognition(),"https://ss0.baidu.com/73F1bjeh1BF3odCf/it/u=507073524,3826295493&fm=85&s=00C2F517435273D00780507D0300C063","http://www.baidu.com");
        newsMessage.addArticle("第二条图文消息","有些爱像大雨滂沱","https://ss0.baidu.com/73F1bjeh1BF3odCf/it/u=507073524,3826295493&fm=85&s=00C2F517435273D00780507D0300C063","http://www.baidu.com");
        newsMessage.addArticle("第三条图文消息","却相信结果","https://ss0.baidu.com/73F1bjeh1BF3odCf/it/u=507073524,3826295493&fm=85&s=00C2F517435273D00780507D0300C063","http://www.baidu.com");
        return newsMessage;
    }

    /**
     * 选项展示
     *
     * @return menuOption
     */
    public String showMenu() {
        StringBuffer sb = new StringBuffer();
        sb.append("-----欢迎订阅该测试公众号-----\n");
        sb.append("如下步骤可以测试部分功能\n");
        sb.append("1、设置备注:格式 remark-备注\n");
        sb.append("2、生成菜单：格式 make-menu\n");
        sb.append("3、删除菜单: 格式 delete-menu\n");
        sb.append("4、发送模板消息: 格式 send-template\n");
        sb.append("5、获取scope为snsapi_base 授权的授权链接: 格式 get-snsapi-base\n");
        sb.append("6、获取scope为snsapi_userinfo 授权的授权链接: 格式 get-snsapi_userinfo\n");
        sb.append("7、获取我的信息: 格式 get-myinfo\n");
        return sb.toString();
    }

    /**
     * 生成菜单
     */
    public void markMenu() {
        Menu menu = new Menu();
        menu.addButton(NormalButton.creaetOne("菜单一")
                .addSubButton(NormalButton.createOne(MenuType.CLICK, "点一点", "点一下"))
                .addSubButton(NormalButton.createOne(MenuType.PIC_PHOTO_OR_ALBUM, "选择并上传图片", "选择图片"))
                .addSubButton(ViewButton.createOne("百度链接", "http://www.baidu.com")));
        menuComponent.createMenu(menu);
    }

}
