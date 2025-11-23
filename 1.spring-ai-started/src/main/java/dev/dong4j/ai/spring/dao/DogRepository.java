package dev.dong4j.ai.spring.dao;

import org.springframework.data.repository.ListCrudRepository;

/**
 * 狗类仓库接口
 *
 * <p>定义了对 Dog 实体进行基础增删改查操作的接口, 继承自 ListCrudRepository 接口, 提供通用的数据库操作方法
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.23
 * @since 1.0.0
 */
interface DogRepository extends ListCrudRepository<Dog, Integer> {}
