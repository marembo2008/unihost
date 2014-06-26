/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unihost.domain.quartz.calendar;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import org.quartz.impl.calendar.MonthlyCalendar;

/**
 *
 * @author marembo
 */
@Entity
public class QuartzMonthlyCalendar extends QuartzCalendar {

  public static class ExcludeDays implements Serializable {

    private boolean[] excludeDays;

    public ExcludeDays() {
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
  private boolean excludeAll = false;

  @Override
  protected void onSaveSelf() {
    MonthlyCalendar mc = (MonthlyCalendar) this.calendar;
    this.excludeAll = mc.areAllDaysExcluded();
    this.excludeDays = new ExcludeDays(mc.getDaysExcluded());
  }

  @Override
  protected void onLoadSelf() {
    MonthlyCalendar mc;
    this.calendar = mc = new MonthlyCalendar();
    mc.setDaysExcluded(excludeDays.excludeDays);
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
