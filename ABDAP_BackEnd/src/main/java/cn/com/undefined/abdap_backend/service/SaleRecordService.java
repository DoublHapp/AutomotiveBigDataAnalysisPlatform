package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.SaleRecordDTO;
import cn.com.undefined.abdap_backend.entity.Region;
import cn.com.undefined.abdap_backend.entity.SaleRecord;
import cn.com.undefined.abdap_backend.repository.RegionRepository;
import cn.com.undefined.abdap_backend.repository.SaleRecordRepository;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 销售记录业务逻辑层
 */
@Service
@Transactional(readOnly = true)
public class SaleRecordService {

    @Autowired
    private SaleRecordRepository repository;

    @Autowired
    private RegionRepository regionRepository;

    /**
     * 查询所有销售记录
     */
    public List<SaleRecordDTO> findAll() {
        List<SaleRecord> saleRecords = repository.findAllWithCarModelAndRegion();
        return saleRecords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据车型ID查询销售记录
     */
    public List<SaleRecordDTO> findByCarModelId(Long carModelId) {
        List<SaleRecord> saleRecords = repository.findByCarModelId(carModelId);
        return saleRecords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据地区ID查询销售记录
     */
    public List<SaleRecordDTO> findByRegionId(Long regionId) {
        List<SaleRecord> saleRecords = repository.findByRegionId(regionId);
        return saleRecords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SaleRecordDTO> findByRegionIds(List<Long> regionId) {
        List<SaleRecord> saleRecords = repository.findByRegionIds(regionId);
        return saleRecords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 指定多个车型id、多个地区id，只返回符合条件的销售记录
     * 
     * @param carModelIds
     * @param regionIds
     * @return 符合条件的销售记录DTO列表
     */
    public List<SaleRecordDTO> getMultipleSaleRecords(List<Long> carModelIds, List<Long> regionIds) {
        List<SaleRecord> saleRecords;
        // 根据参数情况选择不同的查询策略
        if ((carModelIds == null || carModelIds.isEmpty()) && (regionIds == null || regionIds.isEmpty())) {
            // 两个参数都为空，返回所有记录
            saleRecords = repository.findAll();
        } else if (carModelIds == null || carModelIds.isEmpty()) {
            // 只有地区ID参数
            saleRecords = repository.findByRegionIds(regionIds);
        } else if (regionIds == null || regionIds.isEmpty()) {
            // 只有车型ID参数
            saleRecords = repository.findByCarModelIds(carModelIds);
        } else {
            // 两个参数都有值
            saleRecords = repository.findByCarModelIdsAndRegionIds(carModelIds, regionIds);
        }

        // 转换为DTO并返回
        return saleRecords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据地区名称查询销售记录
     */
    public List<SaleRecordDTO> findByRegionName(String regionName) {
        // 先根据地区名称查找地区ID
        Region region = regionRepository.findByRegionName(regionName);
        if (region == null) {
            // 如果地区不存在，返回空列表
            return new ArrayList<>();
        }

        // 根据地区ID查询销售记录
        List<SaleRecord> saleRecords = repository.findByRegionIdWithDetails(region.getRegionId());
        return saleRecords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据车型ID和地区ID查询销售记录
     */
    public List<SaleRecordDTO> getSaleRecordsByCarModelIdAndRegionId(Long carModelId, Long regionId) {
        List<SaleRecord> saleRecords = repository.findByCarModelIdAndRegionId(carModelId, regionId);
        return saleRecords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据车型ID和地区ID查询销售记录（返回原始数据）
     */
    public List<SaleRecord> getSaleRecordsByCarModelIdAndRegionIdRaw(Long carModelId, Long regionId) {
        return repository.findByCarModelIdAndRegionId(carModelId, regionId);
    }

    /**
     * 根据车型ID查询销售记录（返回原始数据）
     */
    public List<SaleRecord> getSaleRecordsByCarModelIdRaw(Long carModelId) {
        return repository.findByCarModelId(carModelId);
    }

    /**
     * 根据车型ID和地区名称（省）查询销售记录（返回原始数据）
     */
    public List<SaleRecord> getSaleRecordsByCarModelIdAndRegionNameRaw(Long carModelId, String regionName) {
        // 先根据地区名称查找所有子地区id
        List<Long> regionIds = regionRepository.findByParentRegion(regionName).stream()
                .map(Region::getRegionId)
                .collect(Collectors.toList());

        // 根据车型ID和地区ID查询销售记录（返回原始数据）
        return repository.findByCarModelIdsAndRegionIds(List.of(carModelId), regionIds);
    }

    /**
     * 根据车型ID和地区名称查询销售记录
     */
    public List<SaleRecordDTO> findByCarModelIdAndRegionName(Long carModelId, String regionName) {
        // 先根据地区名称查找地区ID
        Region region = regionRepository.findByRegionName(regionName);
        if (region == null) {
            // 如果地区不存在，返回空列表
            return new ArrayList<>();
        }

        // 根据车型ID和地区ID查询销售记录
        List<SaleRecord> saleRecords = repository.findByCarModelIdAndRegionId(carModelId, region.getRegionId());
        return saleRecords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将SaleRecord实体转换为SaleRecordDTO
     * 
     * @param saleRecord 销售记录实体
     * @return 销售记录数据DTO
     */
    private SaleRecordDTO convertToDTO(SaleRecord saleRecord) {
        SaleRecordDTO dto = new SaleRecordDTO();
        dto.setSaleId(saleRecord.getSaleId());
        dto.setCarModelId(saleRecord.getCarModelId());
        dto.setRegionId(saleRecord.getRegionId());
        dto.setSaleMonth(saleRecord.getSaleMonth());
        dto.setSaleCount(saleRecord.getSaleCount());
        dto.setSaleAmount(saleRecord.getSaleAmount());
        // 安全设置车型名称
        try {
            if (saleRecord.getCarModel() != null) {
                dto.setCarModelName(saleRecord.getCarModel().getModelName());
            } else {
                dto.setCarModelName(null);
            }
        } catch (EntityNotFoundException e) {
            // 如果获取车型信息失败，设置为null
            dto.setCarModelName(null);
        }

        // 安全设置地区名称
        try {
            if (saleRecord.getRegion() != null) {
                dto.setRegionName(saleRecord.getRegion().getRegionName());
            } else {
                dto.setRegionName(null);
            }
        } catch (EntityNotFoundException e) {
            // 如果获取地区信息失败，设置为null
            dto.setRegionName(null);
        }

        return dto;
    }

}
