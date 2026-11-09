package io.anuke.mindustry.game;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.function.Event;

public class EventType {

    public interface PlayEvent extends Event{
        void handle();
    }

    public interface ResetEvent extends Event{
        void handle();
    }

    public interface WaveEvent extends Event{
        void handle();
    }

    public interface GameOverEvent extends Event{
        void handle();
    }

    public interface StateChangeEvent extends Event{
        void handle(State from, State to);
    }

    public interface FriendlyFireChange extends Event{
        void handle(boolean on);
    }

    public interface BulletEvent extends Event{
        void handle(BulletType type, Entity owner, float x, float y, float angle, short damage);
    }

    public interface EnemyDeathEvent extends Event{
        void handle(Enemy enemy);
    }

    public interface BlockDestroyEvent extends Event{
        void handle(TileEntity entity);
    }

    public interface BlockDamageEvent extends Event{
        void handle(TileEntity entity);
    }

    public interface PlayerDeathEvent extends Event{
        void handle();
    }

    public interface BlockConfigEvent extends Event{
        void handle(Tile tile, byte data);
    }

    public interface BlockTapEvent extends Event{
        void handle(Tile tile);
    }

    public interface WeaponSwitchEvent extends Event{
        void handle();
    }

    public interface UpgradeEvent extends Event{
        void handle(Weapon weapon);
    }

    public interface MessageSendEvent extends Event{
        void handle(String message);
    }

    public interface ShootEvent extends Event{
        void handle(Weapon weapon, float x, float y, float angle);
    }

    /**確定したプレイヤーの配置(オフラインまたはホスト)。マップ生成とセーブ読み込みでは発火しない。
     * block は Block.id: Events.fire() は引数の実行時クラスで handle を探すので、Block のサブクラスは渡せない(Issue #78)*/
    public interface PlaceEvent extends Event{
        void handle(int x, int y, int block, int rotation);
    }

    /**確定したプレイヤーの撤去(オフラインまたはホスト)。block は撤去前のブロックの Block.id*/
    public interface BreakEvent extends Event{
        void handle(int x, int y, int block);
    }

    /**コアへの納品(オフラインまたはサーバー)。item は Item.id、score は残高に加えた得点(負もある)*/
    public interface DeliverEvent extends Event{
        void handle(int item, float score);
    }
}
