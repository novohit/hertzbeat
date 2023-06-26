package org.dromara.hertzbeat.alert.reduce;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hertzbeat.alert.dao.AlertMonitorDao;
import org.dromara.hertzbeat.common.constants.CommonConstants;
import org.dromara.hertzbeat.common.entity.alerter.Alert;
import org.dromara.hertzbeat.common.entity.manager.Tag;
import org.dromara.hertzbeat.common.queue.CommonDataQueue;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * reduce alarm and send alert data
 *
 * @author tom
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlarmCommonReduce {
	
	private final AlarmSilenceReduce alarmSilenceReduce;
	
	private final AlarmConvergeReduce alarmConvergeReduce;
	
	private final CommonDataQueue dataQueue;
	
	private final AlertMonitorDao alertMonitorDao;
	
    public void reduceAndSendAlarm(Alert alert) {
		alert.setTimes(1);
	    Map<String, String> tags = alert.getTags();
	    String monitorIdStr = tags.get(CommonConstants.TAG_MONITOR_ID);
	    if (monitorIdStr == null) {
		    log.error("alert tags monitorId can not be null: {}.", alert);
		    return;
	    }
	    long monitorId = Long.parseLong(monitorIdStr);
	    List<Tag> tagList = alertMonitorDao.findMonitorIdBindTags(monitorId);
		tagList.forEach(tag -> {
			if (!tags.containsKey(tag.getName())) {
				tags.put(tag.getName(), tag.getValue());
			}
		});
		// converge -> silence
	    if (alarmConvergeReduce.filterConverge(alert) && alarmSilenceReduce.filterSilence(alert)) {
			dataQueue.sendAlertsData(alert);
	    }
    }
	
}
