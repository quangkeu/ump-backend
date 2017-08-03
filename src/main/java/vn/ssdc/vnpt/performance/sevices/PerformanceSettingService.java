package vn.ssdc.vnpt.performance.sevices;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.Device;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.dto.AcsResponse;
import vn.ssdc.vnpt.logging.services.ElkService;
import vn.ssdc.vnpt.performance.model.*;
import vn.ssdc.vnpt.utils.StringUtils;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Created by thangnc on 21-Jun-17.
 */
@Service
public class PerformanceSettingService extends SsdcCrudService<Long, PerformanceSetting> {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceSettingService.class);
    private static final String INDEX_PERFORMANCE_STATISTICS = "performance_statitics";
    private static final String TYPE_PERFORMANCE_STATISTICS = "performance_statitics";
    private static final String PARAM_CPU = "InternetGatewayDevice.CpuRam.CpuUsed";
    private static final String PARAM_RAM = "InternetGatewayDevice.CpuRam.RamUsedPer";
    private static final String PARAM_LAN = "LANDevice";
    private static final String PARAM_WAN = "WANDevice";
    private static final String PARAM_WLAN = "WlanAdapter";
    private static final String PARAM_BYTES_RECEIVED = "BytesReceived";
    private static final String PARAM_PACKETS_RECEIVED = "PacketsReceived";
    private static final String PARAM_X_BROADCOM_COM_RXERRORS = "X_BROADCOM_COM_RxErrors";
    private static final String PARAM_X_BROADCOM_COM_RXDROPS = "X_BROADCOM_COM_RxDrops";
    private static final String PARAM_BYTES_SENT = "BytesSent";
    private static final String PARAM_PACKETSSENT = "PacketsSent";
    private static final String PARAM_X_BROADCOM_COM_TXRERRORS = "X_BROADCOM_COM_TxErrors";
    private static final String PARAM_X_BROADCOM_COM_TXDROPS = "X_BROADCOM_COM_TxDrops";
    private static final String PARAM_X_BROADCOM_COM_IFNAME = "X_BROADCOM_COM_IfName";
    private static final String PARAM_X_BROADCOM_COM_WIFINAME = "WlIfcname";

    public static final String INDEX_UMP_PERFORMANCE = "ump_performance";
    public static final String TYPE_UMP_PERFORMANCE = "ump_performance";
    public static final String PRESET = "PERFORMANCE SETTING ";
    public static final String OUI = "InternetGatewayDevice.DeviceInfo.ManufacturerOUI";
    public static final String PRODUCT_CLASS = "InternetGatewayDevice.DeviceInfo.ProductClass";
    public static final String SERIAL_NUMBER = "InternetGatewayDevice.DeviceInfo.SerialNumber";

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    private AcsClient acsClient;

    @Autowired
    private ElkService elkService;

    @Value("${tmpDir}")
    private String tmpDir;

    @Autowired
    public PerformanceSettingService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(PerformanceSetting.class);
    }

    public void createQuartzJob(Long strStartDate, Long strEndDate, Long performanceJobId, Integer intTimeInterval) throws ParseException, SchedulerException {
        Date dStartDate = new Date(strStartDate);
        Date dEndDate = new Date(strEndDate);
        JobDetail job = JobBuilder.newJob(PerformanceQuartzJob.class).withIdentity("Performance_" + performanceJobId).build();
        job.getJobDataMap().put("performanceJobId", performanceJobId);
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("Trigger_Performance_" + performanceJobId)
                .startAt(dStartDate)
                .endAt(dEndDate)
                .withSchedule(simpleSchedule().withIntervalInMinutes(intTimeInterval).repeatForever())
                .build();

        scheduler.scheduleJob(job, trigger);
    }

    public void deleteQuartzJob(Long performanceJobId) throws SchedulerException {
        String strJob = "Performance_".concat(Long.toString(performanceJobId));
        JobKey jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);
    }

    public void deleteTriger(Long performanceJobId) throws SchedulerException {
        String strTrigger = "Trigger_Performance_".concat(Long.toString(performanceJobId));
        TriggerKey triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);
    }


    public List<PerformanceSetting> findByPage(int offset, int limit) {
        String whereExp = "1=1 order by id desc limit ?,? ";
        List<PerformanceSetting> performanceSettings = this.repository.search(whereExp, (offset - 1) * limit, limit);
        return performanceSettings;
    }


    public List<PerformanceSetting> search(String traffic, String monitoring, String startDate, String endDate, String prefix) {
        String whereExp = "1=1";
        if (!("").equals(prefix)) {
            whereExp = " stastics_type like '%" + prefix + "%'";
        } else {
            if (!("").equals(traffic) && !("ALL").equals(traffic))
                whereExp = whereExp + " and stastics_type= '" + traffic + "'";
            if (!("").equals(monitoring) && !("ALL").equals(monitoring))
                whereExp = whereExp + " and monitoring=" + Integer.parseInt(monitoring);
            if (!("").equals(startDate)) whereExp = whereExp + " and start<=" + convertDatetoLong(startDate);
            if (!("").equals(endDate)) whereExp = whereExp + " and end>=" + convertDatetoLong(endDate);
        }
        List<PerformanceSetting> performanceSettings = this.repository.search(whereExp);
        return performanceSettings;
    }

    public long convertDatetoLong(String day) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        long milliseconds = 0;
        try {
            Date d = f.parse(day);
            milliseconds = d.getTime();
        } catch (ParseException e) {
            logger.error("convertDatetoLong ", e);
        }
        return milliseconds;
    }

    public List<PerformanceStatiticsELK> searchPerformanceStatitics(String deviceId, String performanceSettingId, String startDate, String endDate) {
        List<PerformanceStatiticsELK> performanceList = new LinkedList<>();
        try {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("deviceId", deviceId));
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("performanceSettingId", performanceSettingId));

            if (startDate == null || ("").equals(startDate) || endDate == null || ("").equals(endDate)) {
                PerformanceSetting performanceSetting = repository.findOne(Long.parseLong(performanceSettingId));
                startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(performanceSetting.start);
                endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(performanceSetting.end);
            }

            startDate = StringUtils.convertDateToElk(startDate, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            endDate = StringUtils.convertDateToElk(endDate, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            boolQueryBuilder.should(QueryBuilders.rangeQuery("@timestamp")
                    .gte(startDate).lt(endDate)
                    .includeLower(true).includeUpper(true)).minimumShouldMatch("1");

            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.size(9999);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(INDEX_PERFORMANCE_STATISTICS)
                    .addType(TYPE_PERFORMANCE_STATISTICS)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            performanceList = result.getSourceAsObjectList(PerformanceStatiticsELK.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return performanceList;
    }

    public boolean deleteStatiticsInterface(String deviceId, String performanceSettingId, String stasticsInterface) {
        List<PerformanceStatiticsELK> performanceList = new LinkedList<>();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("deviceId", deviceId));
        boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("performanceSettingId", performanceSettingId));
        if (!("").equals(stasticsInterface))
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("valueChanges", stasticsInterface));

        // Call elk to get data
        return elkService.deleteByBoolQuery(boolQueryBuilder, INDEX_PERFORMANCE_STATISTICS, TYPE_PERFORMANCE_STATISTICS);
    }


    private String convertDateToString() {
        Calendar cal = Calendar.getInstance();
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(cal.getTime());
    }

    public List<PerformanceSetting> getListPerformanceByDeviceGroupId(String deviceGroupId) {
        return this.repository.search("device_group_id = ?", deviceGroupId);
    }

    public String exportExcel(String deviceGroupId, String performanceSettingId,
                              String type, String startTime, String endTime, String wanMode,
                              String manufacturer, String modelName, String serialNumber, String monitoring) {
        String strReturn = "ERROR EXPORT !";
        String FILE_NAME = "Traffic_Statistic.xlsx";

        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("DataTable");

            Device[] deviceList = collectDataDevice(deviceGroupId, performanceSettingId,
                    manufacturer, modelName, serialNumber, monitoring);
            String[][] result = null;

            if ("CPU/RAM".contains(type)) {
                result = loadDataRamCpu(performanceSettingId, deviceList, type, startTime, endTime);
                createFormRamCpu(sheet, workbook, result, type);
            } else if ("LAN/WAN/WLAN".contains(type)) {
                result = loadDataLanWanWLan(performanceSettingId, deviceList, type, startTime, endTime, wanMode);
                createFormLanWanWLAN(sheet, workbook, result, wanMode);
            } else if ("STB".contains(type)) {
                createFormStb(sheet, workbook, result);
            } else {
                createFormVoIP(sheet, workbook, result);
            }

            File jsonFile = new File(tmpDir + FILE_NAME);
            FileOutputStream outputStream = new FileOutputStream(jsonFile);
            workbook.write(outputStream);
            workbook.close();

            strReturn = jsonFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            strReturn += e;
        }
        return strReturn;
    }

    private void createFormStb(XSSFSheet sheet, XSSFWorkbook workbook, String[][] result) {
        String[] headerName = {"No", "Manufacturer", "Model name", "Serial number", "IP Address", "Account", "Traffic Received", "Bytes", "Packets"};
        String[] headerPosition = {"A1:A2", "B1:B2", "C1:C2", "D1:D2", "E1:E2", "F1:F2", "G1:H1"};

        Row row = sheet.createRow((short) 0);
        Row row1 = sheet.createRow((short) 1);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
        headerStyle.setAlignment(headerStyle.ALIGN_CENTER);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        XSSFFont font = workbook.createFont();
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        font.setColor(HSSFColor.BLACK.index);
        font.setBold(true);
        headerStyle.setFont(font);
        for (int rowNum = 0; rowNum < headerName.length; rowNum++) {
            if (rowNum > 6) {
                Cell cell = row1.createCell(rowNum - 1);
                cell.setCellValue(headerName[rowNum]);
                cell.setCellStyle(headerStyle);
            } else {
                Cell cell = row.createCell((short) rowNum);
                cell.setCellValue(headerName[rowNum]);
                CellUtil.setAlignment(cell, workbook, CellStyle.VERTICAL_CENTER);
                cell.setCellStyle(headerStyle);
                sheet.addMergedRegion(CellRangeAddress.valueOf(headerPosition[rowNum]));
            }
        }
    }

    private void createFormVoIP(XSSFSheet sheet, XSSFWorkbook workbook, String[][] result) {
        String[] headerName = {"No", "Manufacturer", "Model name", "Serial number", "Call Origin", "Call Destination", "Traffic Received", "Bytes", "Packets"};
        String[] headerPosition = {"A1:A2", "B1:B2", "C1:C2", "D1:D2", "E1:E2", "F1:F2", "G1:H1"};

        Row row = sheet.createRow((short) 0);
        Row row1 = sheet.createRow((short) 1);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
        headerStyle.setAlignment(headerStyle.ALIGN_CENTER);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        XSSFFont font = workbook.createFont();
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        font.setColor(HSSFColor.BLACK.index);
        font.setBold(true);
        headerStyle.setFont(font);
        for (int rowNum = 0; rowNum < headerName.length; rowNum++) {
            if (rowNum > 6) {
                Cell cell = row1.createCell(rowNum - 1);
                cell.setCellValue(headerName[rowNum]);
                cell.setCellStyle(headerStyle);
            } else {
                Cell cell = row.createCell((short) rowNum);
                cell.setCellValue(headerName[rowNum]);
                CellUtil.setAlignment(cell, workbook, CellStyle.VERTICAL_CENTER);
                cell.setCellStyle(headerStyle);
                sheet.addMergedRegion(CellRangeAddress.valueOf(headerPosition[rowNum]));
            }
        }
    }

    private Device[] collectDataDevice(String deviceGroupId, String performanceSettingId, String manufacturer,
                                           String modelName, String serialNumber, String monitoring) {
//        List<Device> deviceList = new ArrayList<Device>();
        Device[] deviceList = null;
        if (monitoring.equals("1")) {
            // single
            deviceList = new Device[1];
            deviceList[0] = new Device();
            Map<String, String> parameters = new HashMap<>();
            parameters.put("_deviceId._Manufacturer", manufacturer);
            parameters.put("summary.modelName", modelName);
            parameters.put("_deviceId._SerialNumber", serialNumber);

            AcsResponse response = new AcsResponse();
            Map<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("query", String.format("{\"_id\":\"/%s/\"}", serialNumber));
            queryParams.put("projection", "_id");
            ResponseEntity<String> responseEntity = this.acsClient.search("devices", queryParams);
            response.body = responseEntity.getBody();

            JsonArray array = new Gson().fromJson(response.body, JsonArray.class);
            JsonObject deviceObject = array.get(0).getAsJsonObject();
            deviceList[0].parameters = parameters;
            deviceList[0].id = deviceObject.get("_id").getAsString();
        } else if (monitoring.equals("2")) {
            // group
            List<Device> deviceList1 = deviceGroupService.getAllListDeviceByGroup(Long.valueOf(deviceGroupId));
            deviceList = deviceList1.stream().toArray(Device[]::new);
        } else {
            // file
            PerformanceSetting performanceSetting = get(Long.valueOf(performanceSettingId));
            List<String> listDeviceIds = new ArrayList<String>();
            for (String deviceId : performanceSetting.externalDevices) {
                listDeviceIds.add(String.format("{\"%s\":\"%s\"}", "_id", deviceId));
            }
            String query = String.format("{\"$or\":[%s]}", org.apache.commons.lang3.StringUtils.join(listDeviceIds, ","));
            AcsResponse response = new AcsResponse();
            Map<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("query", query);
            queryParams.put("projection", "_deviceId._SerialNumber");
            ResponseEntity<String> responseEntity = this.acsClient.search("devices", queryParams);
            response.body = responseEntity.getBody();

            List<Device> deviceList1 = Device.fromJsonString(response.body, deviceIndexParams().keySet());
            deviceList = deviceList1.stream().toArray(Device[]::new);
        }

        return deviceList;
    }
    protected Map<String, String> deviceIndexParams() {
        return new LinkedHashMap<String, String>() {{
            // Infor. map voi key trong file message
            put("_id", "ID");
            put("_deviceId._Manufacturer", "Infor.Manufacturer");
            put("summary.modelName", "Infor.ModelName");
            put("summary.softwareVersion", "Infor.FirmwareVersion");
            put("_tags", "Infor.Label");
            put("_deviceId._SerialNumber", "Infor.SerialNumber");
            put("_registered", "firmwares.created");
            put("_lastInform", "Infor.Updated");
            put("summary.periodicInformInterval", "");
        }};
    }

    private void createFormLanWanWLAN(XSSFSheet sheet, XSSFWorkbook workbook, String[][] result, String wanMode) {
        String title = "";
        if (wanMode.equals("TRANSMITTED")) {
            title = "Traffic Transmitted";
        } else {
            title = "Traffic Received";
        }
        String[] headerName = {"No", "Manufacturer", "Model name", "Serial number", "Interface", title, "Bytes", "Pkts", "Errors", "Drops"};
        String[] headerPosition = {"A1:A2", "B1:B2", "C1:C2", "D1:D2", "E1:E2", "F1:I1"};

        Row row = sheet.createRow((short) 0);
        Row row1 = sheet.createRow((short) 1);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
        headerStyle.setAlignment(headerStyle.ALIGN_CENTER);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        XSSFFont font = workbook.createFont();
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        font.setColor(HSSFColor.BLACK.index);
        font.setBold(true);
        headerStyle.setFont(font);
        for (int rowNum = 0; rowNum < headerName.length; rowNum++) {
            if (rowNum > 5) {
                Cell cell = row1.createCell(rowNum - 1);
                cell.setCellValue(headerName[rowNum]);
                cell.setCellStyle(headerStyle);
            } else {
                Cell cell = row.createCell((short) rowNum);
                cell.setCellValue(headerName[rowNum]);
                CellUtil.setAlignment(cell, workbook, CellStyle.VERTICAL_CENTER);
                cell.setCellStyle(headerStyle);
                sheet.addMergedRegion(CellRangeAddress.valueOf(headerPosition[rowNum]));
            }
        }

        int rowNum = 2;
        for (Object[] datatype : result) {
            Row row2 = sheet.createRow(rowNum++);
            int colNum = 0;
            for (Object field : datatype) {
                Cell cell = row2.createCell(colNum++);
                cell.setCellValue((String) field);
            }
        }
    }

    private void createFormRamCpu(XSSFSheet sheet, XSSFWorkbook workbook, String[][] result, String type) {
        String[] headerName = {"No", "Manufacturer", "Model name", "Serial number", type + " %"};

        Row row = sheet.createRow((short) 0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(HSSFColor.LIGHT_BLUE.index);
        headerStyle.setAlignment(headerStyle.ALIGN_CENTER);
        headerStyle.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
        headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        XSSFFont font = workbook.createFont();
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        font.setColor(HSSFColor.BLACK.index);
        font.setBold(true);
        headerStyle.setFont(font);
        for (int rowNum = 0; rowNum < headerName.length; rowNum++) {
            Cell cell = row.createCell(rowNum);
            cell.setCellValue(headerName[rowNum]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Object[] datatype : result) {
            Row row1 = sheet.createRow(rowNum++);
            int colNum = 0;
            for (Object field : datatype) {
                Cell cell = row1.createCell(colNum++);
                cell.setCellValue((String) field);
            }
        }
    }

    private String[][] loadDataRamCpu(String performanceSettingId, Device[] deviceList, String type, String startTime, String endTime) {

        String[][] dataTable = new String[deviceList.length][5];
        if (deviceList.length > 0) {
            List<String> headers = new ArrayList<String>();
            if (type.equals("RAM")) {
                type = PARAM_RAM;
            } else {
                type = PARAM_CPU;
            }

            for (int i = 0; i < deviceList.length; i++) {
                List<PerformanceStatiticsELK> performanceStatisticsELKS = searchPerformanceStatitics(deviceList[i].id,
                        performanceSettingId, startTime, endTime);
                double count = 0;
                int count1 = 0;
                JsonArray jsonArray = new JsonArray();
                for (int k = 0; k < performanceStatisticsELKS.size(); k++) {

                    if (performanceStatisticsELKS.get(k).parameterNames.contains(type)) {
                        if (headers.size() < performanceStatisticsELKS.size()) {
                            headers.add(StringUtils.convertDateFromElk(performanceStatisticsELKS.get(k).timestamp,
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd HH:mm:ss"));
                        }
                        JsonObject jsonObject1 = new Gson().fromJson(performanceStatisticsELKS.get(k).valueChanges, JsonObject.class);
                        String data = jsonObject1.get(type).toString().replaceAll("\"", "");
                        count += Integer.parseInt(data);

                        JsonObject jsonObjectRow = new JsonObject();
                        jsonObjectRow.addProperty("timeHeader", StringUtils.convertDateFromElk(performanceStatisticsELKS.get(k).timestamp,
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd HH:mm:ss"));
                        jsonObjectRow.addProperty("timeData", data);
                        jsonArray.add(jsonObjectRow);
                        count1++;
                    }

                }
                if (jsonArray.size() != 0) {
                    System.out.println("dataTable.length : " + dataTable.length);
                    dataTable[i][0] = String.valueOf(i + 1);
                    dataTable[i][1] = deviceList[i].parameters.get("_deviceId._Manufacturer");
                    dataTable[i][2] = deviceList[i].parameters.get("summary.modelName");
                    dataTable[i][3] = deviceList[i].parameters.get("_deviceId._SerialNumber");
                    dataTable[i][4] = new DecimalFormat("##.##").format(count / count1);
                }
            }
        }

        return dataTable;
    }

    private String[][] loadDataLanWanWLan(String performanceSettingId, Device[] deviceList, String type, String startTime, String endTime, String wanMode) {

        String[][] dataTable = new String[deviceList.length][5];
        JsonArray arrayLanWanWLanStatistics = new JsonArray();
        if (deviceList.length > 0) {
            String ssid = "";
            String bytesRxTx = PARAM_BYTES_RECEIVED;
            String ptskRxTx = PARAM_PACKETS_RECEIVED;
            String errorsRxTx = PARAM_X_BROADCOM_COM_RXERRORS;
            String dropsRxTx = PARAM_X_BROADCOM_COM_RXDROPS;
            String typeDelete = type;
            if (type.equals("LAN")) {
                type = PARAM_LAN;
                ssid = PARAM_X_BROADCOM_COM_IFNAME;
                if (!wanMode.isEmpty() && wanMode.equals("TRANSMITTED")) {
                    bytesRxTx = PARAM_BYTES_SENT;
                    ptskRxTx = PARAM_PACKETSSENT;
                    errorsRxTx = PARAM_X_BROADCOM_COM_TXRERRORS;
                    dropsRxTx = PARAM_X_BROADCOM_COM_TXDROPS;
                }
            } else if (type.equals("WAN")) {
                type = PARAM_WAN;
                ssid = PARAM_X_BROADCOM_COM_IFNAME;
                if (!wanMode.isEmpty() && wanMode.equals("TRANSMITTED")) {
                    bytesRxTx = PARAM_BYTES_SENT;
                    ptskRxTx = PARAM_PACKETSSENT;
                    errorsRxTx = PARAM_X_BROADCOM_COM_TXRERRORS;
                    dropsRxTx = PARAM_X_BROADCOM_COM_TXDROPS;
                }
            } else {
                type = PARAM_WLAN;
                ssid = PARAM_X_BROADCOM_COM_WIFINAME;
                if (!wanMode.isEmpty() && wanMode.equals("TRANSMITTED")) {
                    bytesRxTx = PARAM_BYTES_SENT;
                    ptskRxTx = PARAM_PACKETSSENT;
                    errorsRxTx = PARAM_X_BROADCOM_COM_TXRERRORS;
                    dropsRxTx = PARAM_X_BROADCOM_COM_TXDROPS;
                }
            }
            List<String> data = new ArrayList<String>();
            for (int i = 0; i < deviceList.length; i++) {
                JsonObject jsonObject = new JsonObject();
                List<PerformanceStatiticsELK> performanceStatisticsELKS = searchPerformanceStatitics(deviceList[i].id,
                        performanceSettingId, startTime, endTime);

                for (int k = 0; k < performanceStatisticsELKS.size(); k++) {
                    String paramLanName = "";
                    double bytes = 0;
                    double ptsk = 0;
                    double errors = 0;
                    double drops = 0;
                    JsonArray jsonArrayParamLan = new JsonArray();
                    if (performanceStatisticsELKS.get(k).parameterNames.contains(type)) {
                        JsonObject jsonObject1 = new Gson().fromJson(performanceStatisticsELKS.get(k).valueChanges, JsonObject.class);

                        for (Map.Entry<String, JsonElement> entry : jsonObject1.entrySet()) {
                            String value = entry.getValue().toString().replaceAll("\"", "");
                            if (entry.getKey().contains(bytesRxTx)) {
                                bytes += Double.valueOf(value);
                            } else if (entry.getKey().contains(ptskRxTx)) {
                                ptsk += Double.valueOf(value);
                            } else if (entry.getKey().contains(errorsRxTx)) {
                                errors += Double.valueOf(value);
                            } else if (entry.getKey().contains(dropsRxTx)) {
                                drops += Double.valueOf(value);
                            }

                            if (!entry.getKey().isEmpty() && entry.getKey().contains(ssid)) {
                                if (jsonObject.get(value) != null) {
                                    jsonArrayParamLan = jsonObject.get(value).getAsJsonArray();
                                    for (int a = 0; a < jsonArrayParamLan.size(); a++) {
                                        JsonObject jsonObject5 = jsonArrayParamLan.get(a).getAsJsonObject();
                                        for (Map.Entry<String, JsonElement> entry1 : jsonObject5.entrySet()) {
                                            String value1 = entry1.getValue().toString().replaceAll("\"", "");
                                            if (entry1.getKey().contains(bytesRxTx)) {
                                                bytes += Double.valueOf(value1);
                                            } else if (entry1.getKey().contains(ptskRxTx)) {
                                                ptsk += Double.valueOf(value1);
                                            } else if (entry1.getKey().contains(errorsRxTx)) {
                                                errors += Double.valueOf(value1);
                                            } else if (entry1.getKey().contains(dropsRxTx)) {
                                                drops += Double.valueOf(value1);
                                            }
                                        }
                                    }

                                }
                                paramLanName = value;
                            }
                        }

                        if (!paramLanName.isEmpty()) {
                            JsonObject jsonObject3 = new JsonObject();
                            jsonObject3.addProperty("bytes", bytes);
                            jsonObject3.addProperty("errors", errors);
                            jsonObject3.addProperty("pkts", ptsk);
                            jsonObject3.addProperty("drops", drops);
                            jsonArrayParamLan.add(jsonObject3);
                            jsonObject.add(paramLanName, jsonArrayParamLan);
                            if (!data.contains(paramLanName)) {
                                data.add(paramLanName);
                            }
                        }
                    }
                }

                jsonObject.addProperty("id", deviceList[i].id);
                jsonObject.addProperty("manufacture", deviceList[i].parameters.get("_deviceId._Manufacturer"));
                jsonObject.addProperty("modelName", deviceList[i].parameters.get("summary.modelName"));
                jsonObject.addProperty("serialNumber", deviceList[i].parameters.get("_deviceId._SerialNumber"));
                arrayLanWanWLanStatistics.add(jsonObject);
            }

            dataTable = collectDataLanWanWlan(arrayLanWanWLanStatistics, typeDelete, data);

        }

        return dataTable;
    }

    private String[][] collectDataLanWanWlan(JsonArray arrayLanWanWLanStatistics, String type, List<String> data) {
        String[][] dataTable = new String[data.size()][9];
        if (type.equals("LAN")) {
            type = "eth";
        } else if (type.equals("WAN")) {
            type = "ve";
        } else {
            type = "wl";
        }

        for (int a = 0; a < arrayLanWanWLanStatistics.size(); a++) {
            JsonObject jsonObject5 = arrayLanWanWLanStatistics.get(a).getAsJsonObject();
            int count = 0;
            for (Map.Entry<String, JsonElement> entry1 : jsonObject5.entrySet()) {

                String value = entry1.getKey().toString().replace("\"", "");
                if (value.startsWith(type)) {
                    JsonArray jsonArray = jsonObject5.get(value).getAsJsonArray();
//                    if (jsonArray.size() > 1) {
                    double bytes = 0;
                    double ptsk = 0;
                    double errors = 0;
                    double drops = 0;
                    for (int x = 0; x < jsonArray.size(); x++) {
                        JsonObject jsonObject = jsonArray.get(x).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry2 : jsonObject.entrySet()) {
                            String value1 = entry2.getValue().toString().replaceAll("\"", "");
                            if (entry2.getKey().contains("bytes")) {
                                bytes += Double.valueOf(value1);
                            } else if (entry2.getKey().contains("pkts")) {
                                ptsk += Double.valueOf(value1);
                            } else if (entry2.getKey().contains("errors")) {
                                errors += Double.valueOf(value1);
                            } else if (entry2.getKey().contains("drops")) {
                                drops += Double.valueOf(value1);
                            }
                        }
                    }

                    dataTable[count][0] = String.valueOf(count + 1);
                    dataTable[count][1] = jsonObject5.get("manufacture").toString().replace("\"", "");
                    dataTable[count][2] = jsonObject5.get("modelName").toString().replace("\"", "");
                    dataTable[count][3] = jsonObject5.get("serialNumber").toString().replace("\"", "");
                    dataTable[count][4] = value;
                    dataTable[count][5] = String.valueOf(bytes);
                    dataTable[count][6] = String.valueOf(ptsk);
                    dataTable[count][7] = String.valueOf(errors);
                    dataTable[count][8] = String.valueOf(drops);
                    count++;
//                    }
                }
            }
        }

        return dataTable;

    }


    public List<String> getListDevicesByPerformanceSettingId(PerformanceSetting performanceSetting) {
        List<String> listDevice = new LinkedList<>();
        if (performanceSetting.monitoring == 1) {
            listDevice.add(performanceSetting.deviceId);
        }
        if (performanceSetting.monitoring == 2) {
            listDevice = deviceGroupService.getListDeviceByGroup(performanceSetting.deviceGroupId);
        }
        if (performanceSetting.monitoring == 3) {
            listDevice = performanceSetting.externalDevices;
        }
        return listDevice;
    }

    public void deletePerformancePreset(Long performanceSettingId){
        acsClient.deletePerformancePreset(PRESET + performanceSettingId);
    }

    public void createPerformancePreset(Long performanceSettingId){
        ResponseEntity<String> responseEntity = acsClient.searchPerformancePreset(PRESET + performanceSettingId);
        if(new Gson().fromJson(responseEntity.getBody(), JsonArray.class).size() == 0){
            PerformanceSetting performanceSetting = get(performanceSettingId);
            PerformancePreset performancePreset = new PerformancePreset();
            performancePreset.channel = "";
            performancePreset.weight = 0;
            performancePreset.schedule = "";

            if (performanceSetting.monitoring == 2) {
                DeviceGroup deviceGroup = deviceGroupService.get(performanceSetting.deviceGroupId);
                performancePreset.precondition = deviceGroup.query;
            }
            if (performanceSetting.monitoring == 3) {
                List<String> listDeviceIds = new ArrayList<String>();
                for (String deviceId : performanceSetting.externalDevices) {
                    String[] deviceIds = deviceId.split("-");
                    listDeviceIds.add(String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\"}", OUI, deviceIds[0], PRODUCT_CLASS, deviceIds[1], SERIAL_NUMBER, deviceIds[2]));
                }
                performancePreset.precondition = String.format("{\"$or\":[%s]}", org.apache.commons.lang3.StringUtils.join(listDeviceIds, ","));
            }
            if (performanceSetting.monitoring == 1) {
                List<String> listDeviceIds = new ArrayList<String>();
                String[] deviceIds = performanceSetting.deviceId.split("-");
                listDeviceIds.add(String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\"}", OUI, deviceIds[0], PRODUCT_CLASS, deviceIds[1], SERIAL_NUMBER, deviceIds[2]));
                performancePreset.precondition = String.format("{\"$or\":[%s]}", org.apache.commons.lang3.StringUtils.join(listDeviceIds, ","));
            }

            List<PerformanceConfiguration> performanceConfigurations = new LinkedList<>();
            List<String> listParameterNames = performanceSetting.parameterNames;
            for (int i = 0; i < listParameterNames.size(); i++) {
                String[] parameterNames = listParameterNames.get(i).split(",");
                for (int j = 0; j < parameterNames.length; j++) {
                    PerformanceConfiguration performanceConfiguration = new PerformanceConfiguration();
                    performanceConfiguration.type = "age";
                    performanceConfiguration.name = parameterNames[j];
                    performanceConfiguration.age = String.valueOf(performanceSetting.stasticsInterval * 60);
                    performanceConfigurations.add(performanceConfiguration);
                }
            }
            performancePreset.configurations = performanceConfigurations;

            Map<String, Boolean> events = new LinkedHashMap<>();
            events.put("2 PERIODIC", true);
            performancePreset.events = events;

            acsClient.createPerformancePreset(performancePreset, PRESET + performanceSettingId);
        }
    }

    public void statiticsData(Long performanceSettingId, String fromDateTime, String endDateTime){
        PerformanceSetting performanceSetting = get(performanceSettingId);
        List<String> listDevice = getListDevicesByPerformanceSettingId(performanceSetting);
        if (listDevice.isEmpty()) return;

        List<String> parameterNames = performanceSetting.parameterNames;
        if(fromDateTime == null) fromDateTime = convertDateToString(performanceSetting.stasticsInterval);

        try {
            boolean isExitValues = false;
            for (int i = 0; i < listDevice.size(); i++) {
                List<PerformanceELK> performanceELKs = getListPerformanceELK(listDevice.get(i), fromDateTime, endDateTime);
                if (!performanceELKs.isEmpty()) {
                    for (int j = 0; j < parameterNames.size(); j++) {
                        Map<String, String> parameterValues = createParameterValues(parameterNames.get(j), performanceELKs);
                        if (!parameterValues.isEmpty()) {
                            createStatiticsELK(listDevice.get(i), performanceSetting, parameterValues, StringUtils.convertDateToElk(convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                            isExitValues = true;
                        }
                    }
                }
                if(!isExitValues) {
                    performanceELKs = getListPerformanceELK(listDevice.get(i), null, null);
                    if (!performanceELKs.isEmpty()) {
                        for (int j = 0; j < parameterNames.size(); j++) {
                            Map<String, String> parameterValues = createParameterValues(parameterNames.get(j), performanceELKs);
                            if (!parameterValues.isEmpty()) {
                                createStatiticsELK(listDevice.get(i), performanceSetting, parameterValues, StringUtils.convertDateToElk(convertDateToString(0), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
                            }
                        }
                    }
                }
                isExitValues = false;
            }
        } catch (Exception e) {
            logger.error("statiticsData", e);
        }
    }

    public Map<String, String> createParameterValues(String parameterName, List<PerformanceELK> performanceELKs) {
        Map<String, String> parameterValues = new LinkedHashMap<>();
        for (int z = 0; z < performanceELKs.size(); z++) {
            String parameter = performanceELKs.get(z).parameterName;
            if (parameterName.contains(parameter) && !parameterValues.containsKey(parameter)) {
                parameterValues.put(parameter, performanceELKs.get(z).value);
            }
        }
        return parameterValues;
    }

    private String convertDateToString(int interval) {
        Calendar cal = Calendar.getInstance();
        if (interval > 0) cal.add(Calendar.HOUR, -interval);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
    }

    public void createStatiticsELK(String deviceId, PerformanceSetting performanceSettings, Map<String, String> parameterValues, String timestamp) {
        Map<String, String> source = new LinkedHashMap<String, String>();
        source.put("@timestamp", timestamp);
        source.put("deviceId", deviceId);
        source.put("stasticsType", performanceSettings.stasticsType);
        source.put("type", performanceSettings.type);
        source.put("performanceSettingId", String.valueOf(performanceSettings.id));
        source.put("parameterNames", new Gson().toJson(performanceSettings.parameterNames));
        source.put("valueChanges", new Gson().toJson(parameterValues));
        try {
            Index index = new Index.Builder(source).index(INDEX_PERFORMANCE_STATISTICS).type(TYPE_PERFORMANCE_STATISTICS).build();
            elasticSearchClient.execute(index);
        } catch (IOException ex) {
            logger.error("createStatiticsELK", ex.toString());
        }
    }

    private List<PerformanceELK> getListPerformanceELK(String deviceId, String fromDateTime, String endDateTime) {
        List<PerformanceELK> performanceELKs = new LinkedList<>();
        try {
            // Create query
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.must(QueryBuilders.matchPhrasePrefixQuery("deviceId", deviceId));

            if (fromDateTime != null && endDateTime != null) {
                boolQueryBuilder.should(QueryBuilders.rangeQuery("@timestamp")
                        .lt(StringUtils.convertDateToElk(endDateTime, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                        .gt(StringUtils.convertDateToElk(fromDateTime, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                        .includeLower(true).includeUpper(true)).minimumShouldMatch("1");
            }
            // Call elk to get data
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder).size(9999);

            Search search = new Search.Builder(searchSourceBuilder.toString())
                    .addIndex(INDEX_UMP_PERFORMANCE)
                    .addType(TYPE_UMP_PERFORMANCE)
                    .addSort(new Sort("@timestamp", Sort.Sorting.DESC))
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            performanceELKs = result.getSourceAsObjectList(PerformanceELK.class);
        } catch (Exception e) {
            logger.error("getListPerformanceELK", e);
        }
        return performanceELKs;
    }
}
