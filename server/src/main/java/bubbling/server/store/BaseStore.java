package bubbling.server.store;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public abstract class BaseStore<T> {

    private final ConcurrentMap<String, T> data = new ConcurrentHashMap<>();
    private final Function<T, String> idExtractor;

    protected BaseStore(Function<T, String> idExtractor) {
        this.idExtractor = Objects.requireNonNull(idExtractor);
    }

    public final synchronized T save(T entity) {
        Objects.requireNonNull(entity, "저장할 객체는 필수입니다.");

        validateSave(entity);

        data.put(idExtractor.apply(entity), entity);

        return entity;
    }

    protected void validateSave(T entity) {

    }

    public final Optional<T> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public final List<T> findAll() {
        return List.copyOf(data.values());
    }

    public final synchronized void delete(String id) {
        data.remove(id);
    }

    public final synchronized void deleteAll(Collection<String> ids) {
        for (String id : ids) {
            data.remove(id);
        }
    }
}