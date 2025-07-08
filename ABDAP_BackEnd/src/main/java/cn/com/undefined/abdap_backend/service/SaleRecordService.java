package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.SaleRecordDTO;
import cn.com.undefined.abdap_backend.entity.Region;
import cn.com.undefined.abdap_backend.entity.SaleRecord;
import cn.com.undefined.abdap_backend.repository.RegionRepository;
import cn.com.undefined.abdap_backend.repository.SaleRecordRepository;
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
        return repository.findAllSaleRecordDTOs();
    }

    /**
     * 根据车型ID查询销售记录
     */
    public List<SaleRecordDTO> findByCarModelId(Long carModelId) {
        return repository.findSaleRecordDTOsByCarModelIds(List.of(carModelId));
    }

    /**
     * 根据地区ID查询销售记录
     */
    public List<SaleRecordDTO> findByRegionId(Long regionId) {
        return repository.findSaleRecordDTOsByRegionIds(List.of(regionId));
    }

    public List<SaleRecordDTO> findByRegionIds(List<Long> regionId) {
        return repository.findSaleRecordDTOsByRegionIds(regionId);
    }

    /**
     * 根据车型ID和地区ID查询销售记录
     */
    public List<SaleRecordDTO> getSaleRecordsByCarModelIdAndRegionId(Long carModelId, Long regionId) {
        return repository.findSaleRecordDTOsByCarModelIdsAndRegionIds(List.of(carModelId), List.of(regionId));
    }

    /**
     * 指定多个车型id、多个地区id，只返回符合条件的销售记录
     * 
     * @param carModelIds
     * @param regionIds
     * @return 符合条件的销售记录DTO列表
     */
    public List<SaleRecordDTO> getMultipleSaleRecords(List<Long> carModelIds, List<Long> regionIds) {
        List<SaleRecordDTO> saleRecordDTOs;
        // 根据参数情况选择不同的查询策略
        if ((carModelIds == null || carModelIds.isEmpty()) && (regionIds == null || regionIds.isEmpty())) {
            // 两个参数都为空，返回所有记录
            saleRecordDTOs = repository.findAllSaleRecordDTOs();
        } else if (carModelIds == null || carModelIds.isEmpty()) {
            // 只有地区ID参数
            saleRecordDTOs = repository.findSaleRecordDTOsByRegionIds(regionIds);
        } else if (regionIds == null || regionIds.isEmpty()) {
            // 只有车型ID参数
            saleRecordDTOs = repository.findSaleRecordDTOsByCarModelIds(carModelIds);
        } else {
            // 两个参数都有值
            saleRecordDTOs = repository.findSaleRecordDTOsByCarModelIdsAndRegionIds(carModelIds, regionIds);
        }

        // 转换为DTO并返回
        return saleRecordDTOs;
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
        return repository.findSaleRecordDTOsByRegionIds(List.of(region.getRegionId()));
    }

    /**
     * 查询所有销售记录（返回原始数据）
     */
    public List<SaleRecord> getAllSaleRecordsRaw() {
        return repository.findAll();
    }

    /**
     * 根据地区ID查询销售记录（返回原始数据）
     */
    public List<SaleRecord> getSaleRecordsByRegionIdRaw(Long regionId) {
        return repository.findByRegionId(regionId);
    }

    /**
     * 根据地区名称（省）查询销售记录（返回原始数据）
     */
    public List<SaleRecord> getSaleRecordsByRegionNameRaw(String regionName) {
        // 先根据地区名称查找子地区ID
        List<Region> regions = regionRepository.findByParentRegion(regionName);
        if (regions == null || regions.isEmpty()) {
            // 如果地区不存在，返回空列表
            return new ArrayList<>();
        }

        // 根据地区ID查询销售记录（返回原始数据）
        return repository.findByRegionIds(regions.stream()
                .map(Region::getRegionId)
                .collect(Collectors.toList()));
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
        return repository.findSaleRecordDTOsByCarModelIdsAndRegionIds(
                List.of(carModelId), List.of(region.getRegionId()));
    }

}
