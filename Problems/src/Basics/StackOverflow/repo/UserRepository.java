package Basics.StackOverflow.repo;

import Basics.StackOverflow.entity.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {

    private final Map<Integer, User> users = new ConcurrentHashMap<>();

    public void save(User user) {
        users.put(user.getId(), user);
    }

    public Optional<User> findById(int id) {
        return Optional.ofNullable(users.get(id));
    }

    public Collection<User> findAll() {
        return users.values();
    }
}