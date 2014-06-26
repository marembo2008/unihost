/**
 * $Id$
 */
package unihost.domain.quartz.trigger;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.quartz.DailyTimeIntervalTrigger;
import static org.quartz.DailyTimeIntervalTrigger.REPEAT_INDEFINITELY;
import org.quartz.DateBuilder;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.impl.triggers.DailyTimeIntervalTriggerImpl;

@Entity
@Table(name = "U_SCHEDULER_TIME_INTERVAL_TRIGGERS")
public class QuartzDailyTimeIntervalTrigger extends QuartzTrigger implements Serializable {

  public static class DaysOfWeek extends HashSet<Integer> {

    public DaysOfWeek() {
    }

    public DaysOfWeek(Collection<? extends Integer> c) {
      super(c);
    }

    public DaysOfWeek(int initialCapacity, float loadFactor) {
      super(initialCapacity, loadFactor);
    }

    public DaysOfWeek(int initialCapacity) {
      super(initialCapacity);
    }

  }
  private static final long serialVersionUID = 1343040349304L;
  private int repeatCount = REPEAT_INDEFINITELY;
  private long repeatInterval = 1;
  private DateBuilder.IntervalUnit repeatIntervalUnit = DateBuilder.IntervalUnit.MINUTE;
  @Lob
  private DaysOfWeek daysOfWeek;
  @Lob
  private TimeOfDay startTimeOfDay;
  @Lob
  private TimeOfDay endTimeOfDay;
  private int timesTriggered = 0;
  private boolean complete = false;

  @Override
  protected void initTrigger() {
    DailyTimeIntervalTriggerImpl st;
    trigger_ = st = new DailyTimeIntervalTriggerImpl();
    st.setRepeatCount(repeatCount);
    st.setRepeatInterval((int) repeatInterval);
    st.setRepeatIntervalUnit(repeatIntervalUnit);
    st.setTimesTriggered(timesTriggered);
    st.setDaysOfWeek(daysOfWeek);
    st.setStartTimeOfDay(startTimeOfDay);
    st.setEndTimeOfDay(endTimeOfDay);
  }

  @Override
  protected void edit(Trigger trigger) {
    if (trigger instanceof DailyTimeIntervalTrigger) {
      DailyTimeIntervalTrigger st = (DailyTimeIntervalTrigger) trigger;
      repeatCount = st.getRepeatCount();
      repeatInterval = st.getRepeatInterval();
      timesTriggered = st.getTimesTriggered();
      complete = !st.mayFireAgain();
      daysOfWeek = new DaysOfWeek(st.getDaysOfWeek());
      startTimeOfDay = st.getStartTimeOfDay();
      endTimeOfDay = st.getEndTimeOfDay();
      repeatIntervalUnit = st.getRepeatIntervalUnit();
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

  public DateBuilder.IntervalUnit getRepeatIntervalUnit() {
    return repeatIntervalUnit;
  }

  public void setRepeatIntervalUnit(DateBuilder.IntervalUnit repeatIntervalUnit) {
    this.repeatIntervalUnit = repeatIntervalUnit;
  }

  public Set<Integer> getDaysOfWeek() {
    return daysOfWeek;
  }

  public void setDaysOfWeek(Set<Integer> daysOfWeek) {
    this.daysOfWeek = new DaysOfWeek(daysOfWeek);
  }

  public TimeOfDay getStartTimeOfDay() {
    return startTimeOfDay;
  }

  public void setStartTimeOfDay(TimeOfDay startTimeOfDay) {
    this.startTimeOfDay = startTimeOfDay;
  }

  public TimeOfDay getEndTimeOfDay() {
    return endTimeOfDay;
  }

  public void setEndTimeOfDay(TimeOfDay endTimeOfDay) {
    this.endTimeOfDay = endTimeOfDay;
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
    return getMisfireInstruction() == DailyTimeIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
  }

}
