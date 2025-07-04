package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.RegionDTO;
import cn.com.undefined.abdap_backend.entity.Region;
import cn.com.undefined.abdap_backend.repository.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 地区数据业务逻辑层
 */
@Service
public class RegionService {

    @Autowired
    private RegionRepository regionRepository;

    /**
     * 获取所有地区数据
     * @return 地区数据DTO列表
     */
    public List<RegionDTO> getAllRegions() {
        List<Region> regions = regionRepository.findAllWithParent();
        return regions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据父级地区ID获取子地区数据
     * @param parentRegionId 父级地区ID
     * @return 子地区数据DTO列表
     */
    public List<RegionDTO> getRegionsByParentId(Long parentRegionId) {
        List<Region> regions = regionRepository.findByParentRegionId(parentRegionId);
        return regions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有顶级地区数据
     * @return 顶级地区数据DTO列表
     */
    public List<RegionDTO> getTopLevelRegions() {
        List<Region> regions = regionRepository.findByParentRegionIdIsNull();
        return regions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询所有非顶级地区数据
     * @return
     */
    public List<RegionDTO> getNonTopLevelRegions() {
        List<Region> regions = regionRepository.findByParentRegionIdIsNotNull();
        return regions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据地区名称搜索地区数据
     * @param regionName 地区名称关键字
     * @return 地区数据DTO列表
     */
    public List<RegionDTO> searchRegionsByName(String regionName) {
        List<Region> regions = regionRepository.findByRegionNameContaining(regionName);
        return regions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将Region实体转换为RegionDTO
     * @param region 地区实体
     * @return 地区数据DTO
     */
    private RegionDTO convertToDTO(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setRegionId(region.getRegionId());
        dto.setRegionName(region.getRegionName());
        dto.setParentRegionId(region.getParentRegionId());
        
        // 设置父级地区名称
        if (region.getParentRegion() != null) {
            dto.setParentRegionName(region.getParentRegion().getRegionName());
        }
        
        return dto;
    }
}
