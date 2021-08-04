package no.unit.nva.useraccessmanagement.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class UserList implements List<UserDto> {

    private final List<UserDto> users;

    @JacocoGenerated
    public UserList() {
        this.users = new ArrayList<>();
    }

    private UserList(List<UserDto> users) {
        this.users = users;
    }

    /**
     * This method avoids creating a copy of the input parameter. The input parameter is the actual list that is
     * contained in the {@link UserList}. The {@link ArrayList} constuctor that accepts a {@link Collection} creates a
     * physical copy of the array that is holding the data.
     *
     * @param users The list that will be contained in the {@link UserList} object
     * @return the created {@link UserList}
     */
    public static UserList fromList(List<UserDto> users) {
        return new UserList(users);
    }

    @Override
    @JacocoGenerated
    public int size() {
        return users.size();
    }

    @Override
    @JacocoGenerated
    public boolean isEmpty() {
        return users.isEmpty();
    }

    @Override
    @JacocoGenerated
    public boolean contains(Object o) {
        return users.contains(o);
    }

    @Override
    @JacocoGenerated
    public Iterator<UserDto> iterator() {
        return users.iterator();
    }

    @Override
    @JacocoGenerated
    public Object[] toArray() {
        return users.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    @JacocoGenerated
    public <T> T[] toArray(T[] a) {
        return users.toArray(a);
    }

    @Override
    @JacocoGenerated
    public boolean add(UserDto userDto) {
        return users.add(userDto);
    }

    @Override
    @JacocoGenerated
    public void add(int index, UserDto element) {
        users.add(index, element);
    }

    @Override
    @JacocoGenerated
    public UserDto remove(int index) {
        return users.remove(index);
    }

    @Override
    @JacocoGenerated
    public boolean remove(Object o) {
        return users.remove(o);
    }

    @Override
    @JacocoGenerated
    public boolean containsAll(Collection<?> c) {
        return users.containsAll(c);
    }

    @Override
    @JacocoGenerated
    public boolean addAll(Collection<? extends UserDto> c) {
        return users.addAll(c);
    }

    @Override
    @JacocoGenerated
    public boolean addAll(int index, Collection<? extends UserDto> c) {
        return users.addAll(index, c);
    }

    @Override
    @JacocoGenerated
    public boolean removeAll(Collection<?> c) {
        return users.removeAll(c);
    }

    @Override
    @JacocoGenerated
    public boolean retainAll(Collection<?> c) {
        return users.retainAll(c);
    }

    @Override
    @JacocoGenerated
    public void clear() {
        users.clear();
    }

    @Override
    @JacocoGenerated
    public UserDto get(int index) {
        return users.get(index);
    }

    @Override
    @JacocoGenerated
    public UserDto set(int index, UserDto element) {
        return users.set(index, element);
    }

    @Override
    @JacocoGenerated
    public int indexOf(Object o) {
        return users.indexOf(o);
    }

    @Override
    @JacocoGenerated
    public int lastIndexOf(Object o) {
        return users.lastIndexOf(o);
    }

    @Override
    @JacocoGenerated
    public ListIterator<UserDto> listIterator() {
        return users.listIterator();
    }

    @Override
    @JacocoGenerated
    public ListIterator<UserDto> listIterator(int index) {
        return users.listIterator(index);
    }

    @Override
    @JacocoGenerated
    public List<UserDto> subList(int fromIndex, int toIndex) {
        return users.subList(fromIndex, toIndex);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserList userDtos = (UserList) o;
        return Objects.equals(users, userDtos.users);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(users);
    }

    public List<UserDto> getWrappedList() {
        return this.users;
    }
}
