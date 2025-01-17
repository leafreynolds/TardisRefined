package whocraft.tardis_refined.common.tardis.control;

import net.minecraft.util.StringRepresentable;
import whocraft.tardis_refined.common.tardis.control.flight.*;
import whocraft.tardis_refined.common.tardis.control.ship.MonitorControl;
import whocraft.tardis_refined.common.tardis.control.ship.ToggleDoorControl;

import java.util.Locale;

public enum ConsoleControl implements StringRepresentable {

    DOOR_TOGGLE("door_toggle", new ToggleDoorControl(), "control.tardis_refined.door_toggle"),
    X("x_cord", new CoordinateControl(CoordinateButton.X), "control.tardis_refined.cord_x"),
    Y("y_cord", new CoordinateControl(CoordinateButton.Y), "control.tardis_refined.cord_y"),
    Z("z_cord", new CoordinateControl(CoordinateButton.Z), "control.tardis_refined.cord_z"),
    INCREMENT("increment", new IncrementControl(), "control.tardis_refined.increment"),
    ROTATE("rotate", new RotationControl(), "control.tardis_refined.rotate"),
    RANDOM("random", new RandomControl(), "control.tardis_refined.random"),
    THROTTLE("throttle", new ThrottleControl(), "control.tardis_refined.throttle"),
    MONITOR("monitor", new MonitorControl(), "control.tardis_refined.monitor"),
    DIMENSION("dimension", new DimensionalControl(), "control.tardis_refined.dimension"),
    FAST_RETURN("fast_return", new FastReturnControl(), "control.tardis_refined.fast_return");

    private final String id;
    private final Control control;
    private final String langId;

    ConsoleControl(String id, Control control, String langId) {
        this.id = id;
        this.control = control;
        this.langId = langId;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public Control getControl() {
        return control;
    }

    public String getTranslationKey() {return langId;}

    public static ConsoleControl findOr(String id, ConsoleControl control) {
        for (ConsoleControl value : ConsoleControl.values()) {
            if (value.name().toLowerCase(Locale.ENGLISH).matches(id.toLowerCase(Locale.ENGLISH))) {
                return value;
            }
        }
        return control;
    }
}
