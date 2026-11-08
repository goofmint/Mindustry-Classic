package io.anuke.mindustry.resource;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Bundles;

public class Item{
	private static final Array<Item> items = new Array<>();

	/** ID順は固定(credit=0、igpu=1、pcie=2、続いて旧アイテム、最後にコード 70 種)。セーブに ID が入るため変えない */
	public static final Item
		credit = new Item("credit"),
		igpu = new Item("igpu"),
		pcie = new Item("pcie");

	/** 旧アイテム。Task 3.11 で削除する */
	public static final Item
		stone = new Item("stone"),
		iron = new Item("iron"),
		coal = new Item("coal"),
		steel = new Item("steel"),
		titanium = new Item("titanium"),
		dirium = new Item("dirium"),
		uranium = new Item("uranium"),
		sand = new Item("sand");
		/*glass = new Item("glass"),
		silicon = new Item("silicon");*/

	/** アイテムIDの上限。Conveyor.ItemPos.toInt/toLong と Save15 がアイテムIDを符号付き byte で保存するため、127 まで */
	public static final int MAX_ITEMS = 127;

	static{
		CodeItem.registerAll();
	}

	public final int id;
	public final String name;
	public TextureRegion region;
	public float explosiveness = 0f;
	public float flammability = 0f;

	public Item(String name) {
		this.id = items.size;
		this.name = name;

		items.add(this);
	}

	public void init(){
		this.region = Draw.region("icon-" + name);
	}

	public String localizedName(){
		return Bundles.get("item." + this.name + ".name");
	}

	@Override
	public String toString() {
		return localizedName();
	}

	public static Array<Item> getAllItems() {
		return Item.items;
	}

	public static Item getByID(int id){
		return items.get(id);
	}

	/** 登録数が上限を超えていれば RuntimeException。起動時に Mindustry.init() の最初で呼ぶ */
	public static void checkRegistrationLimit(){
		checkRegistrationLimit(getAllItems().size);
	}

	/** count が MAX_ITEMS を超えていれば RuntimeException */
	static void checkRegistrationLimit(int count){
		if(count > MAX_ITEMS){
			throw new RuntimeException("Too many items: " + count + " registered, limit is " + MAX_ITEMS
					+ " (item IDs are stored as signed bytes in Conveyor.ItemPos and Save15)");
		}
	}
}
