/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unihost.domain.quartz.calendar;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import org.quartz.impl.calendar.WeeklyCalendar;

/**
 *
 * @author marembo
 */
@Entity
public class QuartzWeeklyCalendar extends QuartzCalendar implements Serializable {

  public static class ExcludeDays implements Serializable {

    private boolean[] excludeDays;

    public ExcludeDays() {
      excludeDays = new boolean[8];
    }

    public ExcludeDays(boolean[] excludeDays) {
      this.excludeDays = excludeDays;
    }

    public boolean[] getExcludeDays() {
      return excludeDays;
    }

    public void setExcludeDays(boolean[] excludeDays) {
      this.excludeDays = excludeDays;
    }

  }
  @Lob
  private ExcludeDays excludeDays;
  // Will be set to true, if all week days are excluded
  private boolean excludeAll;

  @Override
  protected void onSaveSelf() {
    WeeklyCalendar wc = (WeeklyCalendar) this.calendar;
    this.excludeDays = new ExcludeDays(wc.getDaysExcluded());
    this.excludeAll = wc.areAllDaysExcluded();
  }

  @Override
  protected void onLoadSelf() {
    WeeklyCalendar wc;
    this.calendar = wc = new WeeklyCalendar();
    wc.setDaysExcluded(excludeDays.excludeDays);
  }

  public QuartzWeeklyCalendar() {
    excludeDays = new ExcludeDays();
    excludeAll = false;
  }

  public void setExcludeDays(ExcludeDays excludeDays) {
    this.excludeDays = excludeDays;
  }

  public ExcludeDays getExcludeDays() {
    return excludeDays;
  }

  public boolean isExcludeAll() {
    return excludeAll;
  }

  public void setExcludeAll(boolean excludeAll) {
    this.excludeAll = excludeAll;
  }

}
