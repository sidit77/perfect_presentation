package windows.win32.system.com;

import java.lang.foreign.MemorySegment;

public class IUnknownHelper {
    public static MemorySegment as_raw(IUnknown obj) {
        if(obj instanceof IUnknown.$DOWNCALL downcall) {
            return downcall.comObject;
        }
        throw new IllegalArgumentException("Not a native IUnknown object");
    }

}
