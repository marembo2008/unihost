/**
 * $Id$
 */
package unihost.domain.quartz.trigger;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

@Entity
@Table(name = "U_SCHEDULER_SIMPLE_TRIGGERS")
public class QuartzSimpleTrigger extends QuartzTrigger implements Serializable {

  private static final long serialVersionUID = 1L;
  private int repeatCount = 0;
  private long repeatInterval = 0;
  private int timesTriggered = 0;
  private boolean complete = false;

  @Override
  protected void initTrigger() {
    SimpleTriggerImpl st;
    trigger_ = st = new SimpleTriggerImpl();
    st.setRepeatCount(repeatCount);
    st.setRepeatInterval(repeatInterval);
    st.setTimesTriggered(timesTriggered);
  }

  @Override
  protected void edit(Trigger trigger) {
    if (trigger instanceof SimpleTrigger) {
      SimpleTrigger st = (SimpleTrigger) trigger;
      repeatCount = st.getRepeatCount();
      repeatInterval = st.getRepeatInterval();
      timesTriggered = st.getTimesTriggered();
      complete = !st.mayFireAgain();
    }
  }

  public int getRepeatCount() {
    return repeatCount;
  }

  public void setRepeatCount(int repeatCount) {
    this.repeatCount = repeatCount;
  }

  public long getRepeatInterval() {
    return repeatInterval;
  }

  public void setRepeatInterval(long repeatInterval) {
    this.repeatInterval = repeatInterval;
  }

  public int getTimesTriggered() {
    return timesTriggered;
  }

  public void setTimesTriggered(int timesTriggered) {
    this.timesTriggered = timesTriggered;
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete(boolean complete) {
    this.complete = complete;
  }

  @Override
  public boolean canFireNow() {
    return getMisfireInstruction() == SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW
            || getMisfireInstruction() == SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT
            || getMisfireInstruction() == SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT;
  }

}
