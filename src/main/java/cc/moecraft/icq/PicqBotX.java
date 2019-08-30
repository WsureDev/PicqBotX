package cc.moecraft.icq;

import cc.moecraft.icq.accounts.AccountManager;
import cc.moecraft.icq.accounts.AccountManagerListener;
import cc.moecraft.icq.accounts.BotAccount;
import cc.moecraft.icq.accounts.CheakAccount;
import cc.moecraft.icq.command.CommandListener;
import cc.moecraft.icq.command.CommandManager;
import cc.moecraft.icq.event.EventManager;
import cc.moecraft.icq.exceptions.VerifyFailedException;
import cc.moecraft.icq.listeners.HyExpressionListener;
import cc.moecraft.icq.receiver.PicqWebSocketServer;
import cc.moecraft.icq.sender.returndata.returnpojo.get.RVersionInfo;
import cc.moecraft.icq.user.GroupManager;
import cc.moecraft.icq.user.GroupUserManager;
import cc.moecraft.icq.user.UserManager;
import cc.moecraft.logger.HyLogger;
import cc.moecraft.logger.LoggerInstanceManager;
import cc.moecraft.logger.environments.ConsoleColoredEnv;
import cc.moecraft.logger.environments.FileEnv;
import cc.moecraft.logger.format.AnsiColor;
import cc.moecraft.utils.HyExpressionResolver;
import cn.hutool.http.HttpException;
import lombok.Getter;

import java.net.InetSocketAddress;

import static cc.moecraft.icq.PicqConstants.HTTP_API_VERSION_DETECTION;
import static cc.moecraft.icq.PicqConstants.VERSION;
import static cc.moecraft.icq.utils.MiscUtils.logInitDone;
import static cc.moecraft.icq.utils.MiscUtils.logResource;
import static cc.moecraft.logger.format.AnsiColor.GREEN;

/**
 * The class {@code PicqBotX} is the main controller class of all the
 * components of the CoolQ bot program.
 * <p>
 * Class created by the HyDEV Team on 2019-03-23!
 *
 * @author HyDEV Team (https://github.com/HyDevelop)
 * @author Hykilpikonna (https://github.com/hykilpikonna)
 * @author Vanilla (https://github.com/VergeDX)
 * @since 2019-03-23 12:46
 */
@Getter
public class PicqBotX
{
    /**
     * Picq配置 | Picq configuration
     */
    private final PicqConfig config;

    /**
     * WebSocket监听服务器
     */
    private PicqWebSocketServer webSocketServer;

    /**
     * 事件管理器
     */
    private EventManager eventManager;

    /**
     * 机器人账号验证器
     */
    private CheakAccount cheakAccount;

    /**
     * 机器人账号管理器
     */
    private AccountManager accountManager;

    /**
     * 用户对象缓存管理器
     */
    private UserManager userManager;

    /**
     * 群对象缓存管理器
     */
    private GroupManager groupManager;

    /**
     * 群用户对象缓存管理器
     */
    private GroupUserManager groupUserManager;

    /**
     * 指令管理器
     */
    private CommandManager commandManager;

    /**
     * Logger实例管理器
     */
    private LoggerInstanceManager loggerInstanceManager;

    /**
     * Logger
     */
    private HyLogger logger;

    /**
     * 全局替换HyExp表达式 (如果是null就不替换)
     */
    private HyExpressionResolver hyExpressionResolver = null;

    /**
     * 构造器
     *
     * @param config Picq配置
     */
    public PicqBotX(PicqConfig config)
    {
        this(config, true);
    }

    /**
     * 构造器
     *
     * @param config Picq配置
     * @param init 是否启动服务器
     */
    public PicqBotX(PicqConfig config, boolean init)
    {
        this.config = config;
        if (init)
        {
            this.cheakAccount = new CheakAccount() {
                @Override
                public boolean isLegal(BotAccount botAccount) {
                    return super.isLegal(botAccount);
                }
            };
            init();
        }
    }

    /**
     * 构造器
     *
     * @param socketPort 接收端口
     */
    public PicqBotX(int socketPort,CheakAccount cheakAccount)
    {
        this(new PicqConfig(socketPort),cheakAccount);
    }
    /**
     * 构造器
     *
     * @param config Picq配置
     */
    public PicqBotX(PicqConfig config,CheakAccount cheakAccount)
    {
        this(config, true, cheakAccount);
    }

    /**
     * 构造器
     *
     * @param config Picq配置
     * @param init 是否启动服务器
     */
    public PicqBotX(PicqConfig config, boolean init,CheakAccount cheakAccount)
    {
        this.config = config;
        if (init)
        {
            this.cheakAccount = cheakAccount;
            init();
        }
    }

    /**
     * 构造器
     *
     * @param socketPort 接收端口
     */
    public PicqBotX(int socketPort)
    {
        this(new PicqConfig(socketPort));
    }
    /**
     * 初始化
     */
    private void init()
    {
        // 日志管理器
        loggerInstanceManager = new LoggerInstanceManager();
        loggerInstanceManager.addEnvironment(new ConsoleColoredEnv(config.getColorSupportLevel()));
        if (!config.getLogPath().isEmpty())
        {
            loggerInstanceManager.addEnvironment(new FileEnv(config.getLogPath(), config.getLogFileName()));
        }

        // 日志对象
        logger = loggerInstanceManager.getLoggerInstance("PicqBotX", config.isDebug());
        logger.timing.init();
        logResource(logger, config.getColorSupportLevel() == null ? "splash" : "splash-precolored", "version", VERSION);
        logInitDone(logger, "日志管理器     ", 0, 6);

        // 用户和群缓存管理器
        userManager = new UserManager(this);
        groupUserManager = new GroupUserManager(this);
        groupManager = new GroupManager(this);
        logInitDone(logger, "缓存管理器     ", 1, 5);

        // Debug设置没啦w
        logInitDone(logger, "DEBUG设置     ", 2, 4);

        // 事件管理器
        eventManager = new EventManager(this);
        eventManager.registerListener(new HyExpressionListener());
        logInitDone(logger, "事件管理器     ", 3, 3);

        // 账号管理器
        accountManager = new AccountManager();
        eventManager.registerListener(new AccountManagerListener(accountManager));
        logInitDone(logger, "账号管理器     ", 4, 2);

        // HTTP监听服务器
//        httpServer = new PicqHttpServer(config.getSocketPort(), this);

        webSocketServer = new PicqWebSocketServer(new InetSocketAddress("localhost",config.getSocketPort()),this);

        logInitDone(logger, "HTTP监听服务器 ", 5, 1);

        logger.timing.clear();
    }

    /**
     * 添加机器人账号
     *
     * @param name 名字
     * @param token
     * @param secret
     */
    public void addAccount(long id,String name, String token,String secret)
    {
        this.accountManager.addAccount(cheakAccount,new BotAccount(id,name, this,token,secret));
    }
    public void addAccount(BotAccount account)
    {
        this.accountManager.addAccount(cheakAccount,account);
    }
    public void addAccount(Long id)
    {
        if(id!=null)
            this.accountManager.addAccount(cheakAccount,new BotAccount(id,null,null,null,null));
    }

    /**
     * 启动机器人
     */
    public void startBot()
    {
        if (!verifyHttpPluginVersion())
        {
            logger.error("验证失败, 请检查上面的错误信息再重试启动服务器.");
            throw new VerifyFailedException();
        }

        logger.log(GREEN + "正在启动...");
        webSocketServer.start();
    }

    /**
     * 初始化指令管理器
     *
     * @param prefixes 前缀
     */
    public void enableCommandManager(String... prefixes)
    {
        logger.timing.init();

        commandManager = new CommandManager(this, prefixes);
        eventManager.registerListener(new CommandListener(commandManager));
        logInitDone(logger, "指令管理器     ", 6, 0);

        logger.timing.clear();
    }

    /**
     * 验证HTTP插件版本
     *
     * @return 是否通过验证
     */
    public boolean verifyHttpPluginVersion()
    {
        if (config.isNoVerify())
        {
            logger.warning("已跳过版本验证w");
            return true;
        }

        for (BotAccount botAccount : accountManager.getAccounts())
        {
            String prefix = "账号 " + botAccount.getName() + ": ";

            try
            {
                RVersionInfo versionInfo = botAccount.getHttpApi().getVersionInfo().getData();

                if (!versionInfo.getPluginVersion().matches(HTTP_API_VERSION_DETECTION))
                {
                    logger.error(prefix + "HTTP插件版本不正确, 已停止启动");
                    logger.error("- 当前版本: " + versionInfo.getPluginVersion());
                    logger.error("- 兼容的版本: " + HTTP_API_VERSION_DETECTION);
                    return false;
                }

                if (!versionInfo.getCoolqEdition().equalsIgnoreCase("pro"))
                {
                    logger.warning(prefix + "版本正确, 不过用酷Q Pro的话效果更好哦!");
                }
            }
            catch (HttpException e)
            {
                if (e.getMessage().toLowerCase().contains("connection"))
                {
                    logger.error("HTTP发送地址验证失败, 已停止启动");
                    logger.error("- 请检查酷Q是否已经启动");
                    logger.error("- 请检查酷Q的接收端口是否和Picq的发送端口一样");
                    logger.error("- 请检查你的发送IP是不是写错了");
                    logger.error("- 如果是向外, 请检查这个主机有没有网络连接");
                }
                else
                {
                    logger.error("验证失败, HTTP发送错误: ");
                }
                logger.error(e);
                return false;
            }
            logger.log(AnsiColor.YELLOW + prefix + AnsiColor.GREEN + "  版本验证完成!");
        }
        return true;
    }

    /**
     * 设置是否替换HyExp表达式
     *
     * @param value 是否替换
     */
    public void setUniversalHyExpSupport(boolean value)
    {
        setUniversalHyExpSupport(value, true);
    }

    /**
     * 设置是否替换HyExp表达式
     *
     * @param value 是否替换
     * @param safeMode 是否安全模式 (推荐是)
     */
    public void setUniversalHyExpSupport(boolean value, boolean safeMode)
    {
        hyExpressionResolver = value ? new HyExpressionResolver(safeMode) : null;
    }
}
