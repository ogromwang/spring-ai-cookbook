package dev.dong4j.ai.spring.dao;

import org.springframework.data.annotation.Id;

/**
 * 狗的记录类
 *
 * <p>用于表示狗的基本信息, 包含狗的唯一标识, 名称, 主人和描述等属性
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.23
 * @since 1.0.0
 */
record Dog(@Id int id, String name, String owner, String description) {
}
