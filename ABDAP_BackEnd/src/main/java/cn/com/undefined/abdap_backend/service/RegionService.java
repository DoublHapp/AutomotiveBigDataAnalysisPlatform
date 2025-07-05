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
     * 获取所有地区数据（市级）
     * 
     * @return 地区数据DTO列表
     */
    public List<RegionDTO> getAllRegions() {
        List<Region> regions = regionRepository.findAll();
        return regions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据父级地区获取子地区数据
     * 
     * @param parentRegion 父级地区名称
     * @return 子地区数据DTO列表
     */
    public List<RegionDTO> getRegionsByParent(String parentRegion) {
        List<Region> regions = regionRepository.findByParentRegion(parentRegion);
        return regions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据父级地区列表获取子地区数据
     * 
     * @param parentRegions 父级地区名称列表
     * @return 子地区数据DTO列表
     */
    public List<RegionDTO> getRegionsByParent(List<String> parentRegions) {
        List<Region> regions = regionRepository.findByParentRegions(parentRegions);
        return regions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有顶级地区数据（省）
     * 
     * @return 顶级地区名称列表
     */
    public List<String> getTopLevelRegions() {
        List<String> parentRegions = regionRepository.findParentRegions();
        return parentRegions.stream()
                .filter(parentRegion -> parentRegion != null && !parentRegion.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 将Region实体转换为RegionDTO
     * 
     * @param region 地区实体
     * @return 地区数据DTO
     */
    private RegionDTO convertToDTO(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setRegionId(region.getRegionId());
        dto.setRegionName(region.getRegionName());
        dto.setParentRegion(region.getParentRegion());

        return dto;
    }
}
