package cc.moecraft.icq.accounts;

import cc.moecraft.icq.PicqBotX;
import cc.moecraft.icq.sender.IcqHttpApi;
import lombok.Data;
import lombok.Getter;

/**
 * 此类由 Hykilpikonna 在 2018/08/25 创建!
 * Created by Hykilpikonna on 2018/08/25!
 * Github: https://github.com/hykilpikonna
 * QQ: admin@moecraft.cc -OR- 871674895
 *
 * @author Hykilpikonna
 */
@Data
@Getter
public class BotAccount
{
    private final String name;

    private final String secret;

    private long id;

    private final String token;

    private IcqHttpApi httpApi;

    public BotAccount(long id,String name, PicqBotX bot, String secret,String token)
    {
        this.name = name;
        this.secret = secret;
        this.token = token;

        this.httpApi = new IcqHttpApi(bot, this);
        this.id = id;
    }
}
