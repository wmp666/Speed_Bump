package com.wmp.speed_bump.common.ui.components;

import com.wmp.speed_bump.common.background.tools.platform.GetPlatformName;

import java.util.HashMap;
import java.util.Map;

/**
 * 「一份声明、多份平台实现」的面板容器。
 *
 * <h3>怎么用</h3>
 * <p>子类在 {@link #createPanel()} 里把<b>当前平台</b>的面板用 {@link #putPanel(Object)} 登记进来，
 * 使用方只管调 {@link #getPanel()} 拿，不必关心是 Swing 还是 FX：</p>
 * <pre>{@code
 * public class FooPage extends MultiplePanel {
 *     @Override
 *     protected void createPanel() {
 *         putPanel(buildSwingPanel());   // 将来有 FX 实现就再 putPanel 一次
 *     }
 * }
 *
 * JComponent panel = new FooPage().getPanel();
 * }</pre>
 *
 * <h3>{@code createPanel()} 在构造器里就被调用——写子类时必须注意</h3>
 * <p>此时<b>子类自己的字段初始化还没执行</b>，所以下面这种写法会让 {@code putPanel} 收到
 * {@code null}：</p>
 * <pre>{@code
 * private final JPanel panel = new JPanel();   // ← 这行在 createPanel() 之后才跑
 * @Override protected void createPanel() {
 *     putPanel(panel);                          // ← 此时 panel 还是 null
 * }
 * }</pre>
 * <p>正确做法是字段一律<b>不写初始化器</b>，全部在 {@link #createPanel()} 里赋值。
 * {@link #putPanel(Object)} 会拒绝 {@code null} 并抛出带说明的异常，
 * 把这种「静默拿到 null」变成当场报错。</p>
 */
public abstract class MultiplePanel {

    /** 平台名（{@code swing} / {@code fx} / {@code android}）→ 该平台的面板 */
    private final Map<String, Object> multiplePaneMap = new HashMap<>();

    public MultiplePanel() {
        createPanel();
    }

    /** 子类在这里创建并登记本平台的面板 */
    protected abstract void createPanel();

    /**
     * 登记当前平台的面板。
     *
     * @param panel 面板实例，<b>不能为 {@code null}</b>
     * @throws IllegalStateException 传了 {@code null}——十有八九是踩了类注释里说的
     *                               「字段初始化器晚于 {@code createPanel()}」那个坑
     */
    protected final void putPanel(Object panel) {
        if (panel == null) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + ".createPanel() 登记了 null：注意 createPanel() 是在构造器里被调用的，"
                    + "此时子类的字段初始化器还没执行，字段必须在 createPanel() 内部赋值");
        }
        multiplePaneMap.put(GetPlatformName.getUIName(), panel);
    }

    /**
     * 取出当前平台的面板。
     *
     * @return 面板实例；当前平台没有登记过则返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public final <T> T getPanel() {
        return (T) multiplePaneMap.get(GetPlatformName.getUIName());
    }
}
