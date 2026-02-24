package com.example.manager;

import com.example.filter.UserFilter;
import com.example.filter.UserFilters;
import com.example.model.User;
import com.example.sorter.UserSorters;
import org.junit.jupiter.api.*;

import java.util.Comparator;
import java.util.List;

public class UserManagerTest {
    private UserManager userManager;

    @BeforeEach
    public void initUserManager() {
        userManager = new UserManager();
    }

    @Test
    @DisplayName("Успешное добавление нового пользователя")
    public void addNewUser() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);
        Assertions.assertTrue(userManager.findAll().contains(user));
    }

    @Test
    @DisplayName("Добавление существующего пользователя c тем же username - список не должен измениться")
    public void addExistingUser() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        User existingUser = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akuskodan@yandex.ru");

        userManager.add(user);
        userManager.add(existingUser);

        Assertions.assertTrue(userManager.findAll().contains(user));
        Assertions.assertFalse(userManager.findAll().contains(existingUser));
        Assertions.assertEquals(1, userManager.count());
    }

    @Test
    @DisplayName("Успешное удаление пользователя")
    public void removeUser() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);
        userManager.remove(user);
        Assertions.assertFalse(userManager.findAll().contains(user));
    }

    @Test
    @Disabled("Метод поиска пользователей по id некорректный - пропуск")
    public void findUserById() {
    }

    @Test
    @DisplayName("Успешный поиск всех пользователей")
    public void findAllUsers() {
        User user1 = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");
        User user2 = User.validate("AkuskoDanya",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");
        User user3 = User.validate("AkuskoD",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        List<User> expectedUserList = List.of(user1, user2, user3);

        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);

        Assertions.assertTrue(userManager.findAll().containsAll(expectedUserList));
    }

    @Test
    @DisplayName("Успешной поиск пользователя по username")
    public void findUserByUsername() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);

        Assertions.assertTrue(userManager.findByUsername("AkuskoDan").isPresent());
    }

    @Test
    @DisplayName("Успешной поиск не существующего пользователя по username")
    public void findNonExistingUserByUsername() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);

        Assertions.assertTrue(userManager.findByUsername("AkuskoDanil").isEmpty());
    }

    @Test
    @DisplayName("Успешной поиск пользователя по email")
    public void findUserByEmail() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);

        Assertions.assertTrue(userManager.findByEmail("akusko.dan@yandex.ru").isPresent());
    }

    @Test
    @DisplayName("Успешной поиск не существующего пользователя по email")
    public void findNonExistingUserByEmail() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);

        Assertions.assertTrue(userManager.findByEmail("akusko.dan123@yandex.ru").isEmpty());
    }

    @Test
    @DisplayName("Успешный поиск всех пользователей по фильтру")
    public void findAllUsersWithFilter() {
        User user1 = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");
        User user2 = User.validate("AkuskoDanya",
                "Akusko Danil Dmitrievich",
                "akusko.dan@gmail.ru");
        User user3 = User.validate("AkuskoD",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");
        User user4 = User.validate("AkuskoDanilka",
                "Akusko Danil Dmitrievich",
                "akuskodan123@yandex.ru");

        UserFilter filter = UserFilters.byEmailDomain("@yandex.ru");
        List<User> expectedUserList = List.of(user1, user3, user4);

        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);
        userManager.add(user4);

        Assertions.assertTrue(userManager.findByFilter(filter).containsAll(expectedUserList));
    }

    @Test
    @DisplayName("Успешный поиск всех пользователей по фильтру и с учётом порядка")
    public void findAllUsersWithFilterAndSorter() {
        User user1 = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");
        User user2 = User.validate("AkuskoDanya",
                "Akusko Danil Dmitrievich",
                "akusko.dan@gmail.ru");
        User user3 = User.validate("AkuskoD",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");
        User user4 = User.validate("AkuskoDanilka",
                "Akusko Danil Dmitrievich",
                "akuskodan123@yandex.ru");

        UserFilter filter = UserFilters.byEmailDomain("@yandex.ru");
        Comparator<User> sorter = UserSorters.byUsername();
        List<User> expectedUserList = List.of(user3, user1, user4);

        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);
        userManager.add(user4);

        Assertions.assertEquals(expectedUserList, userManager.findAll(filter, sorter));
    }

    @Test
    @DisplayName("Успешная проверка существования пользователя по username")
    public void checkExistingUserByUsername() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);

        Assertions.assertTrue(userManager.exists("AkuskoDan "));
    }

    @Test
    @DisplayName("Успешное обновление существующего пользователя")
    public void updateUserByUserName() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);
        userManager.update("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akuskodanil13@gmail.com");

        Assertions.assertEquals("akuskodanil13@gmail.com", userManager.findAll().getFirst().email());
        Assertions.assertEquals(1, userManager.count());
    }

    @Test
    @DisplayName("Обновление несуществующего пользователя - должно выброситься IllegalArgumentException")
    public void updateNonExistingUserByUserName() {
        User user = User.validate("AkuskoDan",
                "Akusko Danil Dmitrievich",
                "akusko.dan@yandex.ru");

        userManager.add(user);

        Assertions.assertThrows(IllegalArgumentException.class, () -> userManager.update("AkuskoDanil",
                "Akusko Danil Dmitrievich",
                "akuskodanil13@gmail.com"));
    }
}