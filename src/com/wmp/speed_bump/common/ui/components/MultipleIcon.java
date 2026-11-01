package com.wmp.speed_bump.common.ui.components;

import com.wmp.speed_bump.common.background.tools.platform.GetPlatformName;

import java.util.HashMap;
import java.util.Map;

/**
 * 「一份声明、多份平台实现」的图标容器。
 *
 * <h3>怎么用</h3>
 * <p>子类在 {@link #createIcon()} 里把<b>当前平台</b>的图标用 {@link #putIcon(Object)} 登记进来，
 * 使用方只管调 {@link #getIcon()} 拿，不必关心是 Swing 的 {@code ImageIcon} 还是 FX 的 {@code Image}：</p>
 * <pre>{@code
 * public class FooIcon extends MultipleIcon {
 *     @Override
 *     protected void createIcon() {
 *         putIcon(buildSwingIcon());   // 将来有 FX 实现就再 putIcon 一次
 *     }
 * }
 *
 * ImageIcon icon = new FooIcon().getIcon();
 * }</pre>
 *
 * <h3>{@code createIcon()} 在构造器里就被调用——写子类时必须注意</h3>
 * <p>此时<b>子类自己的字段初始化还没执行</b>，所以下面这种写法会让 {@code putIcon} 收到
 * {@code null}：</p>
 * <pre>{@code
 * private final ImageIcon icon = new ImageIcon(...);   // ← 这行在 createIcon() 之后才跑
 * @Override protected void createIcon() {
 *     putIcon(icon);                                    // ← 此时 icon 还是 null
 * }
 * }</pre>
 * <p>正确做法是字段一律<b>不写初始化器</b>，全部在 {@link #createIcon()} 里赋值。
 * {@link #putIcon(Object)} 会拒绝 {@code null} 并抛出带说明的异常，
 * 把这种「静默拿到 null」变成当场报错。</p>
 */
public abstract class MultipleIcon {

    /** 平台名（{@code swing} / {@code fx} / {@code android}）→ 该平台的图标 */
    private final Map<String, Object> multipleIconMap = new HashMap<>();

    public MultipleIcon() {
        createIcon();
    }

    /** 子类在这里创建并登记本平台的图标 */
    protected abstract void createIcon();

    /**
     * 登记当前平台的图标。
     *
     * @param uiName 添加的图标所属UI，为 {@code null} 时回落到当前正在使用的 UI 名
     * @param icon 图标实例，<b>不能为 {@code null}</b>
     * @throws IllegalStateException 传了 {@code null}——十有八九是踩了类注释里说的
     *                               「字段初始化器晚于 {@code createIcon()}」那个坑
     */
    protected final void putIcon(String uiName, Object icon) {
        if (icon == null) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + ".createIcon() 登记了 null：注意 createIcon() 是在构造器里被调用的，"
                    + "此时子类的字段初始化器还没执行，字段必须在 createIcon() 内部赋值");
        }
        multipleIconMap.put(uiName == null ? GetPlatformName.getUIName() : uiName, icon);
    }

    /**
     * 登记当前平台的图标。
     *
     * @param icon 图标实例，<b>不能为 {@code null}</b>
     * @throws IllegalStateException 传了 {@code null}——十有八九是踩了类注释里说的
     *                               「字段初始化器晚于 {@code createIcon()}」那个坑
     */
    protected final void putIcon(Object icon) {
        putIcon(GetPlatformName.getUIName(), icon);
    }

    /**
     * 取出当前平台的图标。
     *
     * @return 图标实例；当前平台没有登记过则返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public final <T> T getIcon() {
        return (T) multipleIconMap.get(GetPlatformName.getUIName());
    }

    public static MultipleIcon quicklyCreate(QuicklyCreator creator){
        return new MultipleIcon() {
            @Override
            protected void createIcon() {
                var info = creator.create();
                putIcon(info.uiName, info.icon);
            }
        };
    }

    public interface QuicklyCreator{
        /**
         *
         * @return 返回值中 {@code uiName} 为null时，默认使用当前正在使用UI名
         */
        QuicklyCreatorInfo create();
    }

    public record QuicklyCreatorInfo(String uiName, Object icon){}
}