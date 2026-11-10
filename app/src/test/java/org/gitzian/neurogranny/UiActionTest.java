package org.gitzian.neurogranny;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.gitzian.neurogranny.view.UiAction;

/** View commands are typed and never encode database IDs inside strings. */
public final class UiActionTest {
    @Test
    public void memorySelectionCarriesItsId() {
        UiAction action = UiAction.selectMemory(123L);

        assertEquals(UiAction.Type.SELECT_MEMORY, action.type());
        assertEquals(123L, action.memoryId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMemoryIdIsRejected() {
        UiAction.selectMemory(0L);
    }

    @Test(expected = IllegalStateException.class)
    public void buttonActionHasNoMemoryId() {
        UiAction.of(UiAction.Type.BACK).memoryId();
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectionRequiresAnId() {
        UiAction.of(UiAction.Type.SELECT_MEMORY);
    }
}
