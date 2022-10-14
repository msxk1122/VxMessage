package com.pt.vx.service;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.pt.vx.config.AllConfig;
import com.pt.vx.config.KeyConfig;
import com.pt.vx.domain.*;
import com.pt.vx.domain.weather.Cast;
import com.pt.vx.domain.weather.WeatherForecastDto;
import com.pt.vx.domain.weather.WeatherLiveDto;
import com.pt.vx.domain.weather.WeatherResponseDto;
import com.pt.vx.utils.ApiUtil;
import com.pt.vx.utils.DateUtil;
import com.pt.vx.utils.VxUtil;
import com.pt.vx.utils.WeatherUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Component
public class MessageService {

    private String message;
    private final List<User> userList =   AllConfig.userList;
    @Scheduled(cron = AllConfig.cron)
    public void setMessage(){

        if(CollectionUtil.isNotEmpty(userList)){
            for(User user : userList){
                sendMessage(user);
                try {
                    Thread.sleep(2333);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(User user){
        VxMessageDto dto = new VxMessageDto();
        dto.setTouser(user.getVx());
        if( !AllConfig.OPEN_MASTER_MODEL || StringUtils.isBlank(message)){
            dto.setTemplate_id(user.getTemplateId());
            HashMap<String, DataInfo> map = new HashMap<>();
            setName(map,user);
            setBirthDay(map,user);
            setWeather(map,user.getAddress(), user.getCity(),AllConfig.OPEN_WEATHER_NOW ?  WeatherUtil.TYPE_LIVE : WeatherUtil.TYPE_ALL);
            setOtherInfo(map);
            if(AllConfig.random_module.isOpen()){
                setRandomInfo(map,user);
            }else {
                setHistoryToday(map);
                setQinghua(map);
                setDongman(map);
                setTiangou(map);
                setWorldRead(map);
                setRandomRead(map);
                setWoZaiRenJian(map);
                setPoetry(map);
                setEnglish(map);
                setMiYu(map);
                setHoroscope(map,user);

            }
            dto.setData(map);
            message = JSONUtil.toJsonStr(dto);
        }
        VxUtil.sendMessage(message);
    }
    private void setBirthDay(HashMap<String, DataInfo> map, User user) {
        BirthDay[] birthDays = user.getBirthDays();
        if(ObjectUtil.isNotEmpty(birthDays)){
            for(int i =0;i<birthDays.length;i++){
                BirthDay birthDay = birthDays[i];
                setMap(map, i==0 ? KeyConfig.KEY_BIRTHDAY : KeyConfig.KEY_BIRTHDAY + i,getBirthDay(birthDay),AllConfig.open_birthDay);
            }
        }
    }
    private void setMiYu(HashMap<String, DataInfo> map) {
        String miyu = ApiUtil.getMiYu();
        setMap(map,KeyConfig.KEY_MI_YU,miyu,AllConfig.open_miyu);
    }
    private void setPoetry(HashMap<String, DataInfo> map) {
        String poetry = ApiUtil.getPoetryApi();
        setMap(map,KeyConfig.KEY_POETRY,poetry,AllConfig.open_poetry);
    }
    private void setWoZaiRenJian(HashMap<String, DataInfo> map) {
        String randomRead = ApiUtil.getWozairenjian();
        setMap(map,KeyConfig.KEY_WO_ZAI_REN_JIAN,randomRead,AllConfig.open_wozairenjian);
    }
    private void setDongman(HashMap<String, DataInfo> map) {
        String randomRead = ApiUtil.getDongman();
        setMap(map,KeyConfig.KEY_DONG_MAN,randomRead,AllConfig.open_dongman);
    }
    private void setRandomInfo(HashMap<String, DataInfo> map,User user){
       String randomInfo;
       int i = RandomUtil.randomInt(110000) % 11;
       if(i == 0 && AllConfig.open_history_today.isOpen()){
           randomInfo = ApiUtil.getHistoryToday(3);
       }else if(i == 1 && AllConfig.open_qinghua.isOpen()){
           randomInfo = ApiUtil.getQingHua();
       }else if(i == 2 && AllConfig.open_dongman.isOpen()){
           randomInfo = ApiUtil.getDongman();
       }else if(i == 3 && AllConfig.open_tiangou.isOpen()){
           randomInfo = ApiUtil.getTgrj();
       }else if(i == 4 && AllConfig.open_world_read.isOpen()){
           randomInfo = ApiUtil.getWorldRead60s();
       }else if(i == 5 && AllConfig.open_random_read.isOpen()){
           randomInfo = ApiUtil.getRandomRead();
       }else if(i == 6 && AllConfig.open_wozairenjian.isOpen()){
           randomInfo = ApiUtil.getWozairenjian();
       }else if(i == 7 && AllConfig.open_poetry.isOpen()){
           randomInfo = ApiUtil.getPoetryApi();
       }else if(i == 8 && AllConfig.open_english.isOpen()){
           randomInfo = ApiUtil.getEnglish();
       }else if(i == 9 && AllConfig.open_miyu.isOpen()){
           randomInfo = ApiUtil.getMiYu();
       }else if(i == 10 && AllConfig.open_horoscope.isOpen() && ObjectUtil.isNotEmpty(user.getBirthDays())){
           randomInfo = ApiUtil.getHoroscopeRead2(user.getBirthDays()[0]);
       }else {
           if(AllConfig.open_history_today.isOpen() || AllConfig.open_qinghua.isOpen()
                   ||AllConfig.open_dongman.isOpen() || AllConfig.open_tiangou.isOpen()
                   || AllConfig.open_world_read.isOpen() ||AllConfig.open_random_read.isOpen()
                   || AllConfig.open_wozairenjian.isOpen() || AllConfig.open_poetry.isOpen()
                   | AllConfig.open_english.isOpen() || AllConfig.open_miyu.isOpen()
                   | AllConfig.open_horoscope.isOpen()){
               setRandomInfo(map,user);
           }
           return;
       }
       setMap(map,KeyConfig.KEY_RANDOM_INFO,randomInfo,AllConfig.random_module);
   }
    private void setRandomRead(HashMap<String, DataInfo> map){
            String randomRead = ApiUtil.getRandomRead();
            setMap(map,KeyConfig.KEY_RANDOM_READ,randomRead,AllConfig.open_random_read);
    }
    private void setWorldRead(HashMap<String, DataInfo> map){
        String worldRead60s = ApiUtil.getWorldRead60s();
        setMap(map,KeyConfig.KEY_WORLD_READ,worldRead60s,AllConfig.open_world_read);
    }
    private void setTiangou(HashMap<String, DataInfo> map){
        String tgrjs = ApiUtil.getTgrj();
        setMap(map,KeyConfig.KEY_TIAN_GOU,tgrjs,AllConfig.open_tiangou);
    }
    private void setQinghua(HashMap<String, DataInfo> map){
        String qingHua = ApiUtil.getQingHua();
        setMap(map,KeyConfig.KEY_QING_HUA,qingHua,AllConfig.open_qinghua);
    }
    private void setEnglish(HashMap<String, DataInfo> map){
        String english = ApiUtil.getEnglish();
        setMap(map,KeyConfig.KEY_ENGLISH,english,AllConfig.open_english);
    }
    private void setHistoryToday(HashMap<String, DataInfo> map){
        String historyToday = ApiUtil.getHistoryToday(3);
        setMap(map,KeyConfig.KEY_HISTORY_TODAY,historyToday,AllConfig.open_history_today);
    }
    private void setHoroscope(HashMap<String, DataInfo> map,User user){
        BirthDay birthDay = user.getBirthDays()[0];
        String horoscopeRead = ApiUtil.getHoroscopeRead(birthDay);
        setMap(map,KeyConfig.KEY_HOROSCOPE,horoscopeRead,AllConfig.open_horoscope);
    }
    private void setName(HashMap<String, DataInfo> map,User user){
        setMap(map, KeyConfig.KEY_USER_NAME,user.getUserName(),AllConfig.open_name);
    }
    private void setWeather(HashMap<String, DataInfo> map,String address,String city,String type){
        if(!AllConfig.open_weather.isOpen()){
            return;
        }
        WeatherResponseDto weather = WeatherUtil.getWeather(address,city,type);
        if(weather == null){
            setWeatherDefault(map);
            return;
        }
        if(WeatherUtil.TYPE_ALL.equals(type)){
            WeatherForecastDto forecast = weather.getForecasts().get(0);
            List<Cast> casts = forecast.getCasts();
            for(int i=0; i<casts.size(); i++ ){
                Cast cast = casts.get(i);//获取天气预报（实际上可以获取到5天的天气预报，0代表今天，之后为1，2，3，4）
                setMap(map,i==0 ? KeyConfig.KEY_WEATHER_DAY : KeyConfig.KEY_WEATHER_DAY + i,cast.getDayweather(),AllConfig.open_weather);//白天天气
                setMap(map,i==0 ? KeyConfig.KEY_TEMPERATURE_DAY : KeyConfig.KEY_TEMPERATURE_DAY +i,cast.getDaytemp() ,AllConfig.open_weather);//白天温度
                setMap(map,i==0 ? KeyConfig.KEY_WEATHER_NIGHT : KeyConfig.KEY_WEATHER_NIGHT +i,cast.getNightweather(),AllConfig.open_weather);//晚上天气
                setMap(map,i==0 ? KeyConfig.KEY_TEMPERATURE_NIGHT : KeyConfig.KEY_TEMPERATURE_NIGHT +i,cast.getNighttemp() ,AllConfig.open_weather);//晚上温度
                setMap(map,i==0 ? KeyConfig.KEY_WIND_DAY : KeyConfig.KEY_WIND_DAY +i,cast.getDaywind(),AllConfig.open_weather);//白天风向
                setMap(map,i==0 ? KeyConfig.KEY_WIND_NIGHT : KeyConfig.KEY_WIND_NIGHT +i,cast.getNightwind() ,AllConfig.open_weather);//晚上风向
                setMap(map,i==0 ?KeyConfig.KEY_POWER_DAY : KeyConfig.KEY_POWER_DAY +i,cast.getDaypower(),AllConfig.open_weather);//白天风力
                setMap(map,i==0 ? KeyConfig.KEY_POWER_NIGHT : KeyConfig.KEY_POWER_NIGHT +i,cast.getNightpower() ,AllConfig.open_weather);//晚上风力
                setMap(map,i==0 ? KeyConfig.KEY_DATE : KeyConfig.KEY_DATE +i,cast.getDate(),AllConfig.open_weather);//日期
                setMap(map,i==0 ? KeyConfig.KEY_WEEK : KeyConfig.KEY_WEEK +i,cast.getWeek() ,AllConfig.open_weather);//星期几
            }
        }else if(WeatherUtil.TYPE_LIVE.equals(type)){
            List<WeatherLiveDto> lives = weather.getLives();
            if(CollUtil.isNotEmpty(lives)){
                WeatherLiveDto liveDto = lives.get(0);
                setMap(map,KeyConfig.KEY_WEATHER_NOW ,liveDto.getWeather(),AllConfig.open_weather);//现在天气
                setMap(map,KeyConfig.KEY_TEMPERATURE_NOW ,liveDto.getTemperature(),AllConfig.open_weather);//现在温度
                setMap(map,KeyConfig.KEY_WIND_NOW,liveDto.getWinddirection(),AllConfig.open_weather);//现在风向
                setMap(map,KeyConfig.KEY_POWER_NOW,liveDto.getProvince(),AllConfig.open_weather);//现在风力
                setMap(map,KeyConfig.KEY_HUMIDITY_NOW ,liveDto.getHumidity(),AllConfig.open_weather);//现在湿度
                setMap(map,KeyConfig.KEY_DATE,liveDto.getReporttime(),AllConfig.open_weather);//现在时间
            }
        }
    }
    private void setOtherInfo(HashMap<String, DataInfo> map){
        String other = "";
        if(AllConfig.open_weather.isOpen()){
            if(AllConfig.OPEN_WEATHER_NOW){
                DataInfo weatherNow = map.get(KeyConfig.KEY_WEATHER_NOW);
                DataInfo temperatureNow = map.get(KeyConfig.KEY_TEMPERATURE_NOW);
                if(weatherNow != null && !StrUtil.equals("未知",weatherNow.getValue()) && weatherNow.getValue().contains("雨")){
                    other = AllConfig.info_weather_prefix_now + AllConfig.info_weather_rain;
                }else if( temperatureNow != null && !StrUtil.equals("未知",temperatureNow.getValue()) && Integer.parseInt(temperatureNow.getValue()) <= 0){
                    other = AllConfig.info_weather_prefix_now + AllConfig.info_weather_temperature_0;
                } else if(temperatureNow != null && !StrUtil.equals("未知",temperatureNow.getValue()) && Integer.parseInt(temperatureNow.getValue()) <= 10){
                    other = AllConfig.info_weather_prefix_now + AllConfig.info_weather_temperature_10;
                }else if( temperatureNow != null && !StrUtil.equals("未知",temperatureNow.getValue()) &&Integer.parseInt(temperatureNow.getValue()) <= 20){
                    other =AllConfig.info_weather_prefix_now + AllConfig.info_weather_temperature_20;
                }
            }
            else {
                DataInfo weatherDay = map.get(KeyConfig.KEY_WEATHER_DAY);
                DataInfo weatherNight = map.get(KeyConfig.KEY_WEATHER_NIGHT);
                DataInfo temperatureDay = map.get(KeyConfig.KEY_TEMPERATURE_DAY);
                DataInfo temperatureNight = map.get(KeyConfig.KEY_TEMPERATURE_NIGHT);
                if(weatherDay != null && !StrUtil.equals("未知",weatherDay.getValue())  && weatherDay.getValue().contains("雨")){
                    other = AllConfig.info_weather_prefix_day + AllConfig.info_weather_rain;
                }else if(weatherNight != null && !StrUtil.equals("未知",weatherNight.getValue()) && weatherNight.getValue().contains("雨")){
                    other = AllConfig.info_weather_prefix_night + AllConfig.info_weather_rain;
                }else if( temperatureDay != null && !StrUtil.equals("未知",temperatureDay.getValue()) &&Integer.parseInt(temperatureDay.getValue()) <= 0){
                    other = AllConfig.info_weather_prefix_day + AllConfig.info_weather_temperature_0;
                }else if(temperatureNight != null && !StrUtil.equals("未知",temperatureNight.getValue()) && Integer.parseInt(temperatureNight.getValue()) <= 0){
                    other = AllConfig.info_weather_prefix_night + AllConfig.info_weather_temperature_0;
                } else if(temperatureDay != null && !StrUtil.equals("未知",temperatureDay.getValue()) && Integer.parseInt(temperatureDay.getValue()) <= 10){
                    other = AllConfig.info_weather_prefix_day + AllConfig.info_weather_temperature_10;
                }else if(temperatureNight != null && !StrUtil.equals("未知",temperatureNight.getValue()) && Integer.parseInt(temperatureNight.getValue()) <= 10){
                    other = AllConfig.info_weather_prefix_night + AllConfig.info_weather_temperature_10;
                } else if(temperatureDay != null && !StrUtil.equals("未知",temperatureDay.getValue()) && Integer.parseInt(temperatureDay.getValue()) < 20){
                    other = AllConfig.info_weather_prefix_day + AllConfig.info_weather_temperature_20;
                }else if(temperatureNight != null && !StrUtil.equals("未知",temperatureNight.getValue()) && Integer.parseInt(temperatureNight.getValue()) < 20){
                    other = AllConfig.info_weather_prefix_night + AllConfig.info_weather_temperature_20;
                }
            }
        }

        if(AllConfig.open_birthDay.isOpen()){
            DataInfo birthday = map.get(KeyConfig.KEY_BIRTHDAY);
            DataInfo loveDay = map.get(KeyConfig.KEY_BIRTHDAY+2);

            if(birthday != null && Objects.equals(Integer.valueOf(birthday.getValue()) , 0)){
                other = AllConfig.info_birthday;
            }else if(loveDay != null && Objects.equals(Integer.valueOf(loveDay.getValue()) , 0)){
                other = AllConfig.info_birthday2;
            }
        }

        setMap(map,KeyConfig.KEY_OTHER_INFO,other,AllConfig.open_other_info);
    }
    private void setWeatherDefault(HashMap<String, DataInfo> map){
        for(int i=0; i<5; i++){
            String ap = "";
            if(i>0){
                ap = i+"";
            }
            setMap(map,"weatherDay"+ap,"未知",AllConfig.open_weather);//白天天气
            setMap(map,"temperatureDay"+ap,"未知",AllConfig.open_weather);//白天温度
            setMap(map,"weatherNight"+ap,"未知",AllConfig.open_weather);//晚上天气
            setMap(map,"temperatureNight"+ap,"未知",AllConfig.open_weather);//晚上温度
            setMap(map,"windDay"+ap,"未知",AllConfig.open_weather);//白天风向
            setMap(map,"windNight"+ap,"未知",AllConfig.open_weather);//晚上风向
            setMap(map,"powerDay"+ap,"未知",AllConfig.open_weather);//白天风力
            setMap(map,"windNight"+ap,"未知" ,AllConfig.open_weather);//晚上风力
            setMap(map,"date"+ap,"未知",AllConfig.open_weather);//日期
            setMap(map,"week"+ap,"未知" ,AllConfig.open_weather);//星期几
        }
        setMap(map,"weatherNow","未知",AllConfig.open_weather);//现在天气
        setMap(map,"temperatureNow","未知",AllConfig.open_weather);//现在温度
        setMap(map,"windNow","未知",AllConfig.open_weather);//现在风向
        setMap(map,"powerNow","未知",AllConfig.open_weather);//现在风力
        setMap(map,"humidityNow","未知",AllConfig.open_weather);//现在湿度
        setMap(map,"date","未知",AllConfig.open_weather);//现在时间
    }
    private String getBirthDay(BirthDay birthDay){
        int year = birthDay.getYear();
        int month = birthDay.getMonth();
        int day = birthDay.getDay();
        if(birthDay.isCountFlag()){
            return birthDay.isChineseFlag()? DateUtil.passDayChinese(year,month,day) : DateUtil.passDay(year,month,day);
        }else {
            return birthDay.isChineseFlag()? DateUtil.getNextChineseBirthDay(month,day) : DateUtil.getNextBirthDay(month,day);
        }

    }
    private void setMap(HashMap<String, DataInfo> map,String key,String value,String color){
        if(ObjectUtil.isEmpty(value) || ObjectUtil.isEmpty(key) || ObjectUtil.isEmpty(color)){
            return;
        }
        if(AllConfig.OPEN_MESSAGE_SPLIT){
            int fontSize = 100;
            int length = value.length();
            BigDecimal len = new BigDecimal(length);
            BigDecimal divide = len.divide(new BigDecimal(fontSize), RoundingMode.UP);
            int size = divide.intValue();
            for(int x=0; x<size;x++){
                int y =  x * 100 + 100;
                if(y > length){
                    y = length;
                }
                String substring = value.substring(x * 100 , y);
                DataInfo dataInfo=new DataInfo();
                dataInfo.setColor(color);
                dataInfo.setValue(substring);
                map.put(x == 0 ? key : key+x ,dataInfo);
            }
        }else {
            DataInfo dataInfo=new DataInfo();
            dataInfo.setColor(color);
            dataInfo.setValue(value);
            map.put(key,dataInfo);
        }
    }
    private void setMap(HashMap<String, DataInfo> map, String key, String value, FunctionConfig config){
        if(config.isOpen()){
            String color = AllConfig.OPEN_RANDOM_COLOR ? getRandomColor() : config.getColor();
            setMap(map,key,value,color);
        }
    }
    private String getRandomColor(){
        if(ObjectUtil.isEmpty(AllConfig.random_colors)){
            return "#000000";
        }
        int length = AllConfig.random_colors.length;
        int i = RandomUtil.randomInt(length * 10000) % length;
        return AllConfig.random_colors[i];
    }



}
