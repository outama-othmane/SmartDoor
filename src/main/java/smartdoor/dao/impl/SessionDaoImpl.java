package smartdoor.dao.impl;

import smartdoor.dao.SessionDao;
import smartdoor.models.Session;
import smartdoor.utils.ConnectionDB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SessionDaoImpl implements SessionDao {

    @Override
    public List<Session> getAll() {
        List<Session> sessions = new ArrayList<Session>();
        ConnectionDB conn = null;
        try {
            conn = new ConnectionDB();

            String query = "SELECT * FROM session";

            PreparedStatement preparedStatement = conn.getConnection().prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Session session = new Session();
                session.setId(resultSet.getInt("id"));
                session.setDate_in(resultSet.getString("date_in"));
                session.setFilename(resultSet.getString("file"));
                sessions.add(session);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.close();
            return sessions;
        }
    }

    @Override
    public void create(Session session) {
        ConnectionDB conn = null;
        try {
            conn = new ConnectionDB();

            String query = "INSERT INTO `session` (`date_in`, `file`) VALUES (?, ?)";
            PreparedStatement preparedStatement = conn.getConnection().prepareStatement(
                    query,
                    Statement.RETURN_GENERATED_KEYS
            );

            preparedStatement.setString(1, session.getDate_in());
            preparedStatement.setString(2, session.getFilename());
            preparedStatement.executeUpdate();

            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                session.setId(resultSet.getInt(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    @Override
    public void delete(Session session) {
        ConnectionDB conn = null;
        try {
            conn = new ConnectionDB();

            String query = "DELETE FROM session WHERE id = ? LIMIT 1";

            PreparedStatement preparedStatement = conn.getConnection().prepareStatement(query);
            preparedStatement.setInt(1, session.getId());
            preparedStatement.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.close();
        }
    }
}