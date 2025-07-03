package cn.com.undefined.abdap_backend.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * CarModelConfig复合主键类
 * 用于处理car_model_id和config_id的联合主键
 */
public class CarModelConfigId implements Serializable {
    
    private Long carModelId;
    private Long configId;
    
    public CarModelConfigId() {}
    
    public CarModelConfigId(Long carModelId, Long configId) {
        this.carModelId = carModelId;
        this.configId = configId;
    }
    
    public Long getCarModelId() {
        return carModelId;
    }
    
    public void setCarModelId(Long carModelId) {
        this.carModelId = carModelId;
    }
    
    public Long getConfigId() {
        return configId;
    }
    
    public void setConfigId(Long configId) {
        this.configId = configId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarModelConfigId that = (CarModelConfigId) o;
        return Objects.equals(carModelId, that.carModelId) && 
               Objects.equals(configId, that.configId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(carModelId, configId);
    }
}
